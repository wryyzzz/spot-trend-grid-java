import api.BinanceApi;
import api.DingDingApi;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.ejlchina.okhttps.HTTP;
import com.ejlchina.okhttps.HttpResult;
import module.Module;
import request.BuyMarketRequest;
import request.GetKlinesRequest;
import request.SellMarketRequest;
import response.BuyMarketResponse;
import response.GetTickerPriceResponse;
import response.SellMarketResponse;
import utils.CalculateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class App extends Module {
    private static final Log log = LogFactory.get();
    private final BinanceApi binanceApi = new BinanceApi(setting().get("binance_api_key"), setting().get("binance_api_secret"));
    private final DingDingApi dingDingApi = new DingDingApi(setting().get("ding_ding_token"));
    private final String coinType = setting().get("coin_type");

    private BigDecimal profitRatio = new BigDecimal(setting().getStr("profit_ratio")).divide(new BigDecimal(100));
    private BigDecimal doubleThrowRatio = new BigDecimal(setting().getDouble("double_throw_ratio")).divide(new BigDecimal(100));

    private BigDecimal nextBuyPrice = new BigDecimal(setting().getDouble("next_buy_price"));
    private BigDecimal gridSellPrice = new BigDecimal(setting().getDouble("grid_sell_price"));

    private Integer curStep = 0;
    private List<BigDecimal> quantity = Arrays.stream(setting().getStr("quantity").split(","))
            .map(BigDecimal::new)
            .collect(Collectors.toList());
    private Deque<BigDecimal> recordPrice = new LinkedList<>();


    public static void main(String[] args) throws Exception {
        new App().start();
    }

    @Override
    public void start() {
        CalculateUtil.setBinanceApi(binanceApi);
        while (true) {
            try {
                GetTickerPriceResponse responseBody = binanceApi.getTickerPrice(coinType);
                if (responseBody != null) {
                    int rightSize = responseBody.price.split("\\.")[1].length();
                    BigDecimal curMarketPrice = new BigDecimal(responseBody.price);
                    int i = nextBuyPrice.compareTo(curMarketPrice);
                    int j = gridSellPrice.compareTo(curMarketPrice);
                    if (i > -1 && CalculateUtil.calculateAngle(coinType, "5m", false, rightSize)) {
                        BuyMarketRequest request = new BuyMarketRequest();
                        request.symbol = coinType;
                        request.type = "MARKET";
                        request.quantity = getQuantity(true).toPlainString();
                        BuyMarketResponse buyMarketResponse = binanceApi.buyMarket(request);
                        if (buyMarketResponse != null && buyMarketResponse.orderId != null) {
                            dingDingApi.dingDingWarn("报警：币种为：%s。买单量为：%s.买单价格为：%s".format(coinType, request.quantity, buyMarketResponse.fills.get(0).price));
                        } else {
                            dingDingApi.dingDingWarn("报警：币种为：%s,买单失败".formatted(coinType));
                        }
                    } else if (j == -1 && CalculateUtil.calculateAngle(coinType, "5m", false, rightSize)) {
                        if (curStep == 0) {
                            modifyPrice(gridSellPrice, curStep, curMarketPrice);
                        } else {
                            BigDecimal lastPrice = recordPrice.getLast();
                            BigDecimal sellAmount = getQuantity(false);
                            BigDecimal profitUsdt = lastPrice.subtract(curMarketPrice).multiply(sellAmount);
                            SellMarketRequest request = new SellMarketRequest();
                            request.symbol = coinType;
                            request.type = "MARKET";
                            request.quantity = getQuantity(false).toPlainString();
                            SellMarketResponse sellMarketResponse = binanceApi.sellMarket(request);
                            if (sellMarketResponse != null && sellMarketResponse.orderId != null) {
                                dingDingApi.dingDingWarn("报警：币种为：%s。卖单量为：%s。预计盈利%sU".formatted(coinType, request.quantity, profitUsdt));
                                setRatio();
                                modifyPrice(recordPrice.getLast(), curStep - 1, curMarketPrice);
                                removeRecordPrice();
                                TimeUnit.SECONDS.sleep(50);
                            }
                        }
                    } else {
                        log.info("当前市价：{}。未能满足交易,继续运行", curMarketPrice);
                    }
                }
                TimeUnit.SECONDS.sleep(2);
            } catch (Exception e) {
                log.error("异常退出", e);
                System.exit(-1);
            }

        }
    }

    private BigDecimal getQuantity(boolean exchangeMethod) {
        int curStep = exchangeMethod ? this.curStep : this.curStep - 1;
        if (curStep < quantity.size()) {
            return curStep == 0 ? quantity.get(0) : quantity.get(curStep);
        } else {
            return quantity.get(quantity.size() - 1);
        }
    }

    private void modifyPrice(BigDecimal dealPrice, int step, BigDecimal marketPrice) {
        //dealPrice*(1-(doubleThrowRatio/100))
        BigDecimal subtract = new BigDecimal(1).subtract(doubleThrowRatio);
        nextBuyPrice = dealPrice.multiply(subtract).setScale(6, RoundingMode.HALF_UP);


        BigDecimal add = new BigDecimal(1).add(profitRatio);
        gridSellPrice = dealPrice.multiply(add).setScale(6, RoundingMode.HALF_UP);
        if (nextBuyPrice.compareTo(marketPrice) == 1) {
            nextBuyPrice = marketPrice.multiply(new BigDecimal(1).subtract(doubleThrowRatio)).setScale(6, RoundingMode.HALF_UP);
        } else if (gridSellPrice.compareTo(marketPrice) == -1) {
            gridSellPrice = marketPrice.multiply(new BigDecimal(1).add(profitRatio)).setScale(6, RoundingMode.HALF_UP);
        }
        curStep = step;
        log.info("修改后的补仓价格为:%s。修改后的网格价格为:%s".formatted(nextBuyPrice, gridSellPrice));
    }


    private void setRatio() {
        GetKlinesRequest request = new GetKlinesRequest();
        request.symbol = coinType;
        request.interval = "4h";
        request.limit = 20;
        List<List<BigDecimal>> klines = binanceApi.getKlines(request);
        BigDecimal percentTotal = new BigDecimal(0);
        for (int i = 0; i < klines.size(); i++) {
            BigDecimal abs = klines.get(i).get(3).subtract(klines.get(i).get(2)).abs();
            percentTotal = abs.divide(klines.get(i).get(4)).add(percentTotal);
        }
        percentTotal = percentTotal.divide(new BigDecimal(2000)).setScale(1, RoundingMode.HALF_UP);
        doubleThrowRatio = percentTotal;
        profitRatio = percentTotal;
    }

    private void removeRecordPrice() {
        recordPrice.removeLast();
    }
}
