package api;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.ejlchina.okhttps.HTTP;
import com.ejlchina.okhttps.HttpResult;
import request.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;


public class BinanceApi {

    private static final HTTP BASE_URL_BUILDER = HTTP.builder().baseUrl("https://www.binance.com/api/v1").build();
    private static final HTTP FUTURE_URL_BUILDER = HTTP.builder().baseUrl("https://fapi.binance.com").build();
    private static final HTTP BASE_URL_V3_BUILDER = HTTP.builder().baseUrl("https://api.binance.com/api/v3").build();
    private static final HTTP PUBLIC_URL_BUILDER = HTTP.builder().baseUrl("https://www.binance.com/exchange/public/product").build();

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.00000000");

    private final String apiKey;
    private final HMac hMac;

    public BinanceApi(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        hMac = new HMac(HmacAlgorithm.HmacSHA256, apiSecret.getBytes());
    }


    public HttpResult getTickerPrice(GetTickerPriceRequest request) {
        return noSignGet(BASE_URL_V3_BUILDER, "/ticker/price", BeanUtil.beanToMap(request));
    }

    public HttpResult getKlines(GetKlinesRequest request) {
        return noSignGet(BASE_URL_BUILDER, "/klines", BeanUtil.beanToMap(request));
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

    public HttpResult buyMarket(BuyMarketRequest request) {
        Map<String, Object> ret = BeanUtil.beanToMap(request);
        ret.put("side", "BUY");
        return signPost(BASE_URL_V3_BUILDER, "/order", ret);

    }

    public HttpResult sellMarket(SellMarketRequest request) {
        Map<String, Object> ret = BeanUtil.beanToMap(request);
        ret.put("side", "SELL");
        return signPost(BASE_URL_V3_BUILDER, "/order", BeanUtil.beanToMap(request));

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

    private Double formatDouble(Double val) {
        return val != null ? Double.valueOf(DECIMAL_FORMAT.format(val)) : val;
    }

    private String sign(Map<String, Object> params) {
        var urlEncode = params.entrySet()
                .stream()
                .map(v -> v.getKey() + "=" + v.getValue())
                .collect(Collectors.joining("&"));
        return hMac.digestHex(urlEncode);
    }


}
