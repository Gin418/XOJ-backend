package com.oj.common.utils;

import com.alibaba.fastjson.JSON;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.oj.common.enums.SmsEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import static com.aliyun.teautil.Common.toJSONString;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * @Author Q
 * @Description TODO
 * @Date 2026/10/16 13:54
 * @Version 1.0.0
 **/
@Component
public class SmsUtil {
    @Value("${aliyun.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aliyun.sms-endpoint:}")
    private String endpoint;

    private static Client client;

    @PostConstruct
    public void init() throws Exception {
        // 未配置短信参数的服务跳过初始化
        if (StringUtils.isAnyBlank(accessKeyId, accessKeySecret, endpoint)) {
            return;
        }
        Config config = new Config()
                // 配置 AccessKey ID，请确保代码运行环境设置了环境变量。
                .setAccessKeyId(accessKeyId)
                // 配置 AccessKey Secret，请确保代码运行环境设置了环境变量。
                .setAccessKeySecret(accessKeySecret);
        // System.getenv()方法表示获取系统环境变量，请配置环境变量后，在此填入环境变量名称，不要直接填入AccessKey信息。

        // 配置 Endpoint
        config.endpoint = endpoint;

        client = new Client(config);
    }

    public static void sendSms(String mobile, SmsEnum smsEnum, Map<String, String> params) {
        try {

            SendSmsRequest sendSmsRequest = new SendSmsRequest()
                            .setPhoneNumbers(mobile)
                            .setSignName("阿里云")
                            .setTemplateCode(smsEnum.getTemplateCode())
                            .setTemplateParam(toJSONString(params));

            // 获取响应对象
            SendSmsResponse sendSmsResponse = client.sendSms(sendSmsRequest);

            // 响应包含服务端响应的 body 和 headers
            System.out.println(toJSONString(sendSmsResponse));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
