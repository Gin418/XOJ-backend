package com.oj.common.constant;

import lombok.Builder;
import lombok.Data;

/**
 * @Author Q
 * @Description TODO
 * @Date 2026/10/11 20:49
 * @Version 1.0.0
 **/
@Data
@Builder
public class GatewayHeaderContext {
    private String UC_TOKEN;
    private String TRACE_ID;
    private String SPAN_ID;
    private Long X_USER_ID;
    private String X_USER_NAME;
    private String X_USER_ROLE;
    private Long X_HOTEL_ID;
    private String X_HOTEL_CODE;
    private Long X_HOTEL_GROUP_ID;
    private String X_HOTEL_GROUP_CODE;
    private String X_CHANNEL_CODE;
    private String X_CLIENT_IP;
    private String X_VALUE;

    GatewayHeaderContext(String UC_TOKEN, String TRACE_ID, String SPAN_ID, Long X_USER_ID, String X_USER_NAME, String X_USER_ROLE, Long X_HOTEL_ID, String X_HOTEL_CODE, Long X_HOTEL_GROUP_ID, String X_HOTEL_GROUP_CODE, String X_CHANNEL_CODE, String X_CLIENT_IP, String X_VALUE) {
        this.UC_TOKEN = UC_TOKEN;
        this.TRACE_ID = TRACE_ID;
        this.SPAN_ID = SPAN_ID;
        this.X_USER_ID = X_USER_ID;
        this.X_USER_NAME = X_USER_NAME;
        this.X_USER_ROLE = X_USER_ROLE;
        this.X_HOTEL_ID = X_HOTEL_ID;
        this.X_HOTEL_CODE = X_HOTEL_CODE;
        this.X_HOTEL_GROUP_ID = X_HOTEL_GROUP_ID;
        this.X_HOTEL_GROUP_CODE = X_HOTEL_GROUP_CODE;
        this.X_CHANNEL_CODE=X_CHANNEL_CODE;
        this.X_CLIENT_IP=X_CLIENT_IP;
        this.X_VALUE=X_VALUE;
    }
}
