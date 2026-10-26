package com.oj.common.utils;

import com.oj.common.constant.GatewayConsts;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * @Author Q
 * @Description TODO
 * @Date 2026/10/11 18:58
 * @Version 1.0.0
 **/
public class Tools {
    /**
     * 生成流水号
     *
     * @return
     */
    public static String generateUuid() {
        return UUID.randomUUID().toString().replaceAll("-", StringUtils.EMPTY);
    }

    /**
     * 字符串UTF-8编码
     *
     * @param val
     */
    public static String encodeUtf8(String val) {
        String result = StringUtils.EMPTY;
        if (StringUtils.isNotBlank(val)) {
            try {
                result = URLEncoder.encode(val, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                // UTF-8 为 JDK 内置编码，理论上不会抛出该异常
            }
        }
        return result;
    }


    /**
     * 获取请求头信息Map
     *
     * @param headers
     * @return
     */
    public static Map<String, String> getHeaderMap(HttpHeaders headers) {
        Map<String, String> result = new HashMap<>();
        if (headers != null && !headers.isEmpty()) {
            String val;
            Iterator<Entry<String, List<String>>> it = headers.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, List<String>> en = it.next();
                val = StringUtils.EMPTY;
                if (en.getValue() != null && !en.getValue().isEmpty()) {
                    val = StringUtils.join(en.getValue(), ',');
                }
                result.put(en.getKey(), val);
            }
        }
        return result;
    }

    public static String buildCacheKey(String... params) {
        return buildKey(":", params);
    }

    public static String buildKey(String separator, String... params) {
        String result = "";
        if (params != null && params.length > 0) {
            result = org.apache.commons.lang3.StringUtils.join(params, separator);
        }
        return result;
    }

    /**
     * 获取头信息
     *
     * @param headers
     * @param key
     * @return
     */
    public static String getHeader(HttpHeaders headers, String key) {
        String result = org.apache.commons.lang3.StringUtils.EMPTY;
        if (headers.containsKey(key)) {
            List<String> list = headers.get(key);
            if (list != null && !list.isEmpty()) {
                result = org.apache.commons.lang3.StringUtils.join(list, GatewayConsts.SEPARATOR_COMMA);
            }
        }
        return result;
    }
}

