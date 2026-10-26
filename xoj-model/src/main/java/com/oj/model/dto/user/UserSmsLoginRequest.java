package com.oj.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 验证码登录请求
 */
@Data
public class UserSmsLoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 短信验证码
     */
    private String code;
}
