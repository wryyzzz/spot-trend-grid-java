package api;

import cn.hutool.core.bean.BeanUtil;
import com.ejlchina.okhttps.HTTP;
import com.ejlchina.okhttps.HttpResult;
import com.ejlchina.okhttps.JacksonMsgConvertor;
import request.DingDingMsgRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DingDingApi {
    private static final HTTP DING_DING_URL_BUILDER = HTTP.builder().baseUrl("https://oapi.dingtalk.com")
            .addMsgConvertor(new JacksonMsgConvertor())
            .build();
    private final Map<String, String> tokenUrlParam = new HashMap<>();

    public DingDingApi(String token) {
        tokenUrlParam.put("access_token", token);
    }

    public HttpResult dingDingWarn(String msg) {
        DingDingMsgRequest request = new DingDingMsgRequest();
        request.msgtype = "text";
        request.at = new DingDingMsgRequest.At(Boolean.FALSE, Set.of("11111"));
        request.text = new DingDingMsgRequest.Text(msg);
        Map<String, Object> ret = BeanUtil.beanToMap(request);
        ret.put("at", BeanUtil.beanToMap(request.at));
        ret.put("text", BeanUtil.beanToMap(request.text));
        HttpResult result = DING_DING_URL_BUILDER.sync("/robot/send")
                .addHeader("Content-Type", "application/json;charset=utf-8")
                .addUrlPara(tokenUrlParam)
                .bodyType("json")
                .setBodyPara(ret)
                .post();
        result.close();
        return result;
    }
}
