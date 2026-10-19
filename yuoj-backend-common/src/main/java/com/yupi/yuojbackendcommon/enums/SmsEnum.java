package com.yupi.yuojbackendcommon.enums;

/**
 * @Author Q
 * @Description TODO
 * @Date 2026/10/16 13:55
 * @Version 1.0.0
 **/
public enum SmsEnum {

    SEND_CAPTCHA_CODE("SMS_305433711");

    private String templateCode;

    SmsEnum(String templateCode) {
        this.templateCode = templateCode;
    }
    public String getTemplateCode() {
        return templateCode;
    }
}
