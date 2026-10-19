package com.yupi.yuojbackendmodel.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户找回密码请求体
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
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
