package com.yupi.yuojbackendcommon.enums;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @Author Q
 * @Description TODO
 * @Date 2026/9/29 0:08
 * @Version 1.0.0
 **/
public enum OSSEnum {
    USER("xoj-914", "https://xoj-914.oss-cn-hangzhou.aliyuncs.com/");

    //桶名称
    public final String bucketName;

    //原生URL
    public final String rawUrl;

    OSSEnum (String bucketName, String rawUrl) {
        this.bucketName = bucketName;
        this.rawUrl = rawUrl;
    }

    private static Map<String, OSSEnum> map = new HashMap<>();

    static {
        for (OSSEnum obsEnum : OSSEnum.values()) {
            map.put(obsEnum.bucketName, obsEnum);
        }
    }

    public static OSSEnum getById(String value) {
        return map.get(value);
    }

    public static Set<String> keys() {
        return map.keySet();
    }
}
