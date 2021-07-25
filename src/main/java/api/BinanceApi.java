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

    private static final Map<HTTP, Integer> CALL_NUMBER = new HashMap<>();

    private static final Deque<String> USER_AGENT = new LinkedList<>();

    private final String apiKey;
    private final HMac hMac;

    public BinanceApi(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        hMac = new HMac(HmacAlgorithm.HmacSHA256, apiSecret.getBytes());
        CALL_NUMBER.put(BASE_URL_BUILDER, 0);
        CALL_NUMBER.put(FUTURE_URL_BUILDER, 0);
        CALL_NUMBER.put(BASE_URL_V3_BUILDER, 0);
        CALL_NUMBER.put(PUBLIC_URL_BUILDER, 0);
        Collections.addAll(USER_AGENT, "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_8; en-us) AppleWebKit/534.50 (KHTML, like Gecko) Version/5.1 Safari/534.50",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.835.163 Safari/535.1",
                "Opera/9.80 (Windows NT 6.1; U; zh-cn) Presto/2.9.168 Version/11.50",
                "Mozilla/5.0 (Windows; U; Windows NT 6.1; ) AppleWebKit/534.12 (KHTML, like Gecko) Maxthon/3.0",
                "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.41 Safari/5",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36",
                "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0)",
                "Mozilla/5.0 (Macintosh; U; PPC Mac OS X 10.5; en-US; rv:1.9.2.15) Gecko/20110303 Firefox/3.6.15");
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
        HttpResult httpResult = signPost(BASE_URL_V3_BUILDER, "/order", ret);
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
        urlParam.put("recvWindow", 55000);
        urlParam.put("timestamp", System.currentTimeMillis() / 1000L);
        String signature = sign(urlParam);
        urlParam.put("signature", signature);

        return http.sync(url)
                .addUrlPara(urlParam)
                .addHeader("User-Agent", getUserAgent())
                .addHeader("X-MBX-APIKEY", apiKey)
//                .addHeader("Connection", "close")
                .get();


    }

    private HttpResult signPost(HTTP http, String url, Map<String, Object> bodyParam) {
        bodyParam.put("recvWindow", 55000);
        bodyParam.put("timestamp", System.currentTimeMillis() / 1000L);
        String signature = sign(bodyParam);
        bodyParam.put("signature", signature);
        return http.sync(url)
                .addBodyPara(bodyParam)
                .addHeader("User-Agent", getUserAgent())
//                .addHeader("Connection", "close")
                .addHeader("X-MBX-APIKEY", apiKey)
                .post();
    }

    private HttpResult noSignGet(HTTP http, String url, Map<String, Object> urlParam) {
        return http.sync(url)
                .addUrlPara(urlParam)
                .addHeader("User-Agent", getUserAgent())
//                .addHeader("Connection", "close")
                .get();

    }

    private HttpResult noSignPost(HTTP http, String url, Map<String, Object> bodyParam) {
        return http.sync(url)
                .addHeader("User-Agent", getUserAgent())
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
        return hMac.digestHex(urlEncode);
    }

    private String getUserAgent() {
        String userAgent = USER_AGENT.removeLast();
        USER_AGENT.addFirst(userAgent);
        return userAgent;
    }


}
