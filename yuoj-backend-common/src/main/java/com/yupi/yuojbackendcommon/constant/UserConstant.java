package com.yupi.yuojbackendcommon.constant;

/**
 * 用户常量
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public interface UserConstant {

    /**
     * 用户登录态 Redis 键前缀（token 关联）
     */
    String USER_LOGIN_TOKEN_KEY = "yuoj:login:token:";

    /**
     * 登录态过期时间：30 天（秒）
     */
    long USER_LOGIN_EXPIRE_SECONDS = 30L * 24 * 60 * 60;

    /**
     * 短信验证码 Redis 键前缀（手机号关联）
     */
    String USER_CAPTCHA_KEY = "yuoj:user:captcha:";

    /**
     * 邮箱验证码 Redis 键前缀（邮箱关联）
     */
    String USER_EMAIL_CAPTCHA_KEY = "yuoj:user:email:captcha:";

    /**
     * 短信验证码过期时间：5 分钟（秒）
     */
    long USER_CAPTCHA_EXPIRE_SECONDS = 5L * 60;

    //  region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 被封号
     */
    String BAN_ROLE = "ban";

    // endregion
}
