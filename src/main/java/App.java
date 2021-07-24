import api.BinanceApi;
import api.DingDingApi;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import cn.hutool.setting.Setting;
import com.ejlchina.okhttps.HttpResult;
import module.Module;
import request.GetPositionInfoRequest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class App extends Module {
    private static final Log log = LogFactory.get();
    private BinanceApi binanceApi = new BinanceApi(setting().get("binance_api_key"), setting().get("binance_api_secret"));
    private DingDingApi dingDingApi = new DingDingApi(setting().get("ding_ding_token"));
    private String coinType = setting().get("coin_type");
    private Double profitRatio = setting().getDouble("profit_ratio");
    private Double doubleThrowRatio = setting().getDouble("double_throw_ratio");
    private Double nextBuyPrice = setting().getDouble("next_buy_price");
    private Double gridSellPrice = setting().getDouble("grid_sell_price");
    private Set<BigDecimal> quantity = Arrays.stream(setting().getStr("quantity").split(","))
            .map(BigDecimal::new).collect(Collectors.toSet());



    public static void main(String[] args) {
        new App().start();
    }

    @Override
    public void start() {
        while (true) {

        }
    }
}
