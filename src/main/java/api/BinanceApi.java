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
import com.ejlchina.okhttps.internal.SyncHttpTask;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import request.*;
import response.BuyMarketResponse;
import response.GetTickerPriceResponse;
import response.SellMarketResponse;
import utils.UserAgentUtil;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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

    private static final Map<HTTP, Integer> CALL_NUMBER = new HashMap<>();


    private final String apiKey;
    private final HMac hMac;

    public BinanceApi(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        hMac = new HMac(HmacAlgorithm.HmacSHA256, apiSecret.getBytes(StandardCharsets.UTF_8));
        CALL_NUMBER.put(BASE_URL_BUILDER, 0);
        CALL_NUMBER.put(FUTURE_URL_BUILDER, 0);
        CALL_NUMBER.put(BASE_URL_V3_BUILDER, 0);
        CALL_NUMBER.put(PUBLIC_URL_BUILDER, 0);

    }


    public GetTickerPriceResponse getTickerPrice(String coinType) {
        HttpResult httpResult = noSignGet(BASE_URL_V3_BUILDER, "/ticker/price", Map.of("symbol", coinType));
        try {
            if (httpResult.getStatus() == 200) {
                return httpResult.getBody().toBean(GetTickerPriceResponse.class);
            } else {
                log.error("获取当前虚拟货币价格失败,coinType:{},body:{}", coinType, httpResult.getBody());
                return null;
            }
        } catch (Exception e) {
            log.error("获取当前虚拟货币价格异常", e);
            return null;
        } finally {
            httpResult.close();
        }

    }

    public List<List<BigDecimal>> getKlines(GetKlinesRequest request) {
        HttpResult httpResult = noSignGet(BASE_URL_BUILDER, "/klines", BeanUtil.beanToMap(request));
        try {
            if (httpResult.getStatus() == 200) {
                return httpResult.getBody().toBean(new TypeRef<>() {
                    @Override
                    public Type getType() {
                        return super.getType();
                    }
                });
            } else {
                log.error("获取k线图失败,request:{},body:{}", request, httpResult.getBody());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("获取K线图异常", e);
            return null;
        } finally {
            httpResult.close();
        }
    }

    public BuyMarketResponse buyMarket(BuyMarketRequest request) {
        request.quantity = formatDouble(request.quantity);
        Map<String, Object> ret = BeanUtil.beanToMap(request);
        ret.put("side", "BUY");
        HttpResult httpResult = signPost(BASE_URL_V3_BUILDER, "/order/test", ret);
        try {
            if (httpResult.getStatus() == 200) {
                return httpResult.getBody().toBean(BuyMarketResponse.class);
            } else {
                log.error("挂买单失败,request:{},body:{}", request, httpResult.getBody());
                return null;
            }
        } catch (Exception e) {
            log.error("挂买单异常", e);
            return null;
        } finally {
            httpResult.close();
        }

    }

    public SellMarketResponse sellMarket(SellMarketRequest request) {
        request.quantity = formatDouble(request.quantity);
        Map<String, Object> ret = BeanUtil.beanToMap(request);
        ret.put("side", "SELL");
        HttpResult httpResult = signPost(BASE_URL_V3_BUILDER, "/order", BeanUtil.beanToMap(request));
        try {
            if (httpResult.getStatus() == 200) {
                return httpResult.getBody().toBean(SellMarketResponse.class);
            } else {
                log.error("挂卖单失败,request:{},body:{}", request, httpResult.getBody());
                return null;
            }
        } catch (Exception e) {
            log.error("挂卖单失败", e);
            return null;
        } finally {
            httpResult.close();
        }
    }


    private HttpResult signGet(HTTP http, String url, Map<String, Object> urlParam) {
        urlParam.put("recvWindow", 5000);
        urlParam.put("timestamp", System.currentTimeMillis());
        String signature = sign(urlParam);
        urlParam.put("signature", signature);

        return http.sync(url)
                .addUrlPara(urlParam)
                .addHeader("User-Agent", UserAgentUtil.getUserAgent())
                .addHeader("X-MBX-APIKEY", apiKey)
//                .addHeader("Connection", "close")
                .get();


    }

    private HttpResult signPost(HTTP http, String url, Map<String, Object> bodyParam) {
        bodyParam.put("recvWindow", 5000);
        bodyParam.put("timestamp", System.currentTimeMillis());
        String signature = sign(bodyParam);
        bodyParam.put("signature", signature);
        return http.sync(url)
                .addBodyPara(bodyParam)
                .addHeader("User-Agent", UserAgentUtil.getUserAgent())
//                .addHeader("Connection", "close")
                .addHeader("X-MBX-APIKEY", apiKey)
                .post();
    }

    private HttpResult noSignGet(HTTP http, String url, Map<String, Object> urlParam) {
        return http.sync(url)
                .addUrlPara(urlParam)
                .addHeader("User-Agent", UserAgentUtil.getUserAgent())
//                .addHeader("Connection", "close")
                .get();

    }

    private HttpResult noSignPost(HTTP http, String url, Map<String, Object> bodyParam) {
        return http.sync(url)
                .addHeader("User-Agent", UserAgentUtil.getUserAgent())
//                .addHeader("Connection", "close")
                .addBodyPara(bodyParam)
                .post();
    }

    private String formatDouble(String val) {
        return val != null ? new BigDecimal(val).setScale(8, RoundingMode.HALF_UP).toPlainString() : val;
    }

    private String sign(Map<String, Object> params) {
        var urlEncode = params.entrySet()
                .stream()
                .map(v -> v.getKey() + "=" + v.getValue())
                .collect(Collectors.joining("&"));
        urlEncode = urlEncode.substring(0, urlEncode.length() - 2);
        return hMac.digestHex(urlEncode);
    }

}
