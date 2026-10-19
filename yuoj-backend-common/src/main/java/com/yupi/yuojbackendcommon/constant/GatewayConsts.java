package com.yupi.yuojbackendcommon.constant;

/**
 * @Author Q
 * @Description TODO
 * @Date 2026/10/11 19:08
 * @Version 1.0.0
 **/
public class GatewayConsts {
    public static final String CONTENT_TYPE_JSON = "application/json;charset=utf-8";
    public static final String SEPARATOR_COMMA = ",";
    public static final String HEADER_TRACE_ID = "TRACE-ID";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_USER_ID = "X-USER-ID";
    public static final String HEADER_USER_NAME = "X-USER-NAME";
    public static final String HEADER_USER_MOBILE = "X-USER-MOBILE";
    public static final String HEADER_HOTEL_ID = "X-HOTEL-ID";
    public static final String HEADER_HOTEL_CODE = "X-HOTEL-CODE";
    public static final String HEADER_GROUP_ID = "X-GROUP-ID";
    public static final String HEADER_GROUP_CODE = "X-GROUP-CODE";
    public static final String HEADER_USER_ROLE = "X-USER-ROLE";
    public static final String HEADER_CLIENT_IP = "X_CLIENT_IP";
    public static final String ERROR_MERCHANT_LOCK = "GATEWAY_MERCHANT_LOCK";
    public static final String ERROR_UC = "GATEWAY_UC";
    public static final String ERROR_NOT_ALLOW = "GATEWAY_NOT_ALLOW";
    public static final String ERROR_LOST_TOKEN = "GATEWAY_LOST_TOKEN";
    public static final String ERROR_UNKNOWN = "GATEWAY_UNKNOWN";
    public static final String HEADER_TOKEN = "uc-token";

    /**
     * 未登录时网关透传给下游服务的占位用户 id（哨兵值，非真实用户）
     */
    public static final long NOT_LOGIN_USER_ID = -999L;
}
