package com.oj.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 发送短信验证码请求
 */
@Data
public class SmsCaptchaSendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 手机号
     */
    private String phone;
}
