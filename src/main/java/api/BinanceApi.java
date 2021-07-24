package api;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.ejlchina.data.Array;
import com.ejlchina.data.TypeRef;
import com.ejlchina.okhttps.HTTP;
import com.ejlchina.okhttps.HttpResult;
import com.ejlchina.okhttps.JacksonMsgConvertor;
import request.*;
import response.BuyMarketResponse;
import response.GetTickerPriceResponse;
import response.SellMarketResponse;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ResponseCache;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;


public class BinanceApi {
    private static final Log log = LogFactory.get();

    private static final HTTP BASE_URL_BUILDER = HTTP.builder().baseUrl("https://www.binance.com/api/v1")
            .addMsgConvertor(new JacksonMsgConvertor()).build();
    private static final HTTP FUTURE_URL_BUILDER = HTTP.builder().baseUrl("https://fapi.binance.com")
            .addMsgConvertor(new JacksonMsgConvertor()).build();
    private static final HTTP BASE_URL_V3_BUILDER = HTTP.builder().baseUrl("https://api.binance.com/api/v3")
            .addMsgConvertor(new JacksonMsgConvertor()).build();
    private static final HTTP PUBLIC_URL_BUILDER = HTTP.builder().baseUrl("https://www.binance.com/exchange/public/product")
            .addMsgConvertor(new JacksonMsgConvertor()).build();

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.00000000");

    private final String apiKey;
    private final HMac hMac;

    public BinanceApi(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        hMac = new HMac(HmacAlgorithm.HmacSHA256, apiSecret.getBytes());
    }


    public GetTickerPriceResponse getTickerPrice(String coinType) {
        HttpResult httpResult = noSignGet(BASE_URL_V3_BUILDER, "/ticker/price", Map.of("symbol", coinType));
        if (httpResult.getStatus() == 200) {
            return httpResult.getBody().toBean(GetTickerPriceResponse.class);
        } else {
            log.error("获取当前虚拟货币价格失败,coinType:{},response:{},body:{}", coinType, httpResult, httpResult.getBody());
            return null;
        }
    }

    public List<List<BigDecimal>> getKlines(GetKlinesRequest request) {
        HttpResult httpResult = noSignGet(BASE_URL_BUILDER, "/klines", BeanUtil.beanToMap(request));
        if (httpResult.getStatus() == 200) {
            return httpResult.getBody()
                    .toBean(new TypeRef<>() {
                        @Override
                        public Type getType() {
                            return super.getType();
                        }
                    });
        } else {
            log.error("获取k线图失败,request:{},response:{},body:{}", request, httpResult, httpResult.getBody());
            return Collections.emptyList();
        }
    }

    public HttpResult buyLimit(BuyLimitRequest request) {
        request.quantity = formatDouble(request.quantity);
        request.price = formatDouble(request.price);
        Map<String, Object> ret = BeanUtil.beanToMap(request);
        ret.put("side", "BUY");
        return signPost(BASE_URL_V3_BUILDER, "/order", ret);
    }

    public HttpResult sellLimit(SellLimitRequest request) {
        request.quantity = formatDouble(request.quantity);
        request.price = formatDouble(request.price);
        Map<String, Object> ret = BeanUtil.beanToMap(request);
        ret.put("side", "SELL");
        return signPost(BASE_URL_V3_BUILDER, "/order", ret);
    }

    public BuyMarketResponse buyMarket(BuyMarketRequest request) {
        Map<String, Object> ret = BeanUtil.beanToMap(request);
        ret.put("side", "BUY");
        HttpResult httpResult = signPost(BASE_URL_V3_BUILDER, "/order", ret);
        if (httpResult.getStatus() == 200) {
            return httpResult.getBody().toBean(BuyMarketResponse.class);
        } else {
            log.error("挂买单失败,request:{},response:{},body:{}", request, httpResult, httpResult.getBody());
            return null;
        }

    }

    public SellMarketResponse sellMarket(SellMarketRequest request) {
        Map<String, Object> ret = BeanUtil.beanToMap(request);
        ret.put("side", "SELL");
        HttpResult httpResult = signPost(BASE_URL_V3_BUILDER, "/order", BeanUtil.beanToMap(request));
        if (httpResult.getStatus() == 200) {
            return httpResult.getBody().toBean(SellMarketResponse.class);
        }else {
            log.error("挂卖单失败,request:{},response:{},body:{}", request, httpResult, httpResult.getBody());
            return null;
        }
    }

    public HttpResult getTicker24hour(GetTicker24hourRequest request) {
        return noSignGet(BASE_URL_BUILDER, "/ticker/24hr", BeanUtil.beanToMap(request));
    }

    public HttpResult getPositionInfo(GetPositionInfoRequest request) {
        return signGet(BASE_URL_BUILDER, "/positionRisk", BeanUtil.beanToMap(request));
    }

    public HttpResult getFuturePositionInfo(GetFuturePositionInfoRequest request) {
        return signGet(FUTURE_URL_BUILDER, "/fapi/v2/positionRisk", BeanUtil.beanToMap(request));
    }

    private HttpResult signGet(HTTP http, String url, Map<String, Object> urlParam) {
        urlParam.put("recvWindow", 5000);
        urlParam.put("timestamp", System.currentTimeMillis() / 1000L);
        String signature = sign(urlParam);
        urlParam.put("signature", signature);
        return http.sync(url)
                .addUrlPara(urlParam)
                .addHeader("X-MBX-APIKEY", apiKey)
                .get();
    }

    private HttpResult signPost(HTTP http, String url, Map<String, Object> bodyParam) {
        bodyParam.put("recvWindow", 5000);
        bodyParam.put("timestamp", System.currentTimeMillis() / 1000L);
        String signature = sign(bodyParam);
        bodyParam.put("signature", signature);
        return http.sync(url)
                .addBodyPara(bodyParam)
                .addHeader("X-MBX-APIKEY", apiKey)
                .post();
    }

    private HttpResult noSignGet(HTTP http, String url, Map<String, Object> urlParam) {
        return http.sync(url)
                .addUrlPara(urlParam)
                .get();
    }

    private HttpResult noSignPost(HTTP http, String url, Map<String, Object> bodyParam) {
        return http.sync(url)
                .addBodyPara(bodyParam)
                .get();
    }

    private BigDecimal formatDouble(BigDecimal val) {
        return val != null ? val.setScale(8, RoundingMode.HALF_UP) : val;
    }

    private String sign(Map<String, Object> params) {
        var urlEncode = params.entrySet()
                .stream()
                .map(v -> v.getKey() + "=" + v.getValue())
                .collect(Collectors.joining("&"));
        return hMac.digestHex(urlEncode);
    }


}
