package com.oj.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户找回密码请求体
 */
@Data
public class UserResetPasswordRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120794L;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 邮箱验证码
     */
    private String captcha;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}
