package com.oj.common.utils;

import com.google.gson.Gson;
import com.oj.common.constant.GatewayConsts;
import com.oj.common.constant.UserConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author Q
 * @Description 登录用户信息工具类：根据 token 从 Redis 中获取登录用户
 * @Date 2026/10/11 20:05
 * @Version 1.0.0
 **/
public class LoginUserUtil {

    private static final Gson GSON = new Gson();

    /**
     * 登录校验通过后，用户 id 在 request attribute 中的存储键
     */
    public static final String LOGIN_USER_ID_ATTRIBUTE = "LOGIN_USER_ID";

    /**
     * 根据 token 从 Redis 中获取登录用户信息（供 user-service、gateway 等服务复用）
     *
     * @param stringRedisTemplate
     * @param token               token
     * @param clazz               用户实体类型
     * @return 未登录、token 无效或登录态过期时返回 null
     */
    public static <T> T getLoginUser(StringRedisTemplate stringRedisTemplate, String token, Class<T> clazz) {
        if (StringUtils.isBlank(token) || !JwtUtil.verifyToken(token)) {
            return null;
        }
        String userJson = stringRedisTemplate.opsForValue().get(UserConstant.USER_LOGIN_TOKEN_KEY + token);
        if (StringUtils.isBlank(userJson)) {
            return null;
        }
        return GSON.fromJson(userJson, clazz);
    }

    /**
     * 判断 token 是否有效且登录态仍存在于 Redis 中
     *
     * @param stringRedisTemplate
     * @param token
     * @return
     */
    public static boolean isLogin(StringRedisTemplate stringRedisTemplate, String token) {
        if (StringUtils.isBlank(token) || !JwtUtil.verifyToken(token)) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(UserConstant.USER_LOGIN_TOKEN_KEY + token));
    }

    /**
     * 从请求头中获取 token（支持 Authorization: Bearer xxx，回退 uc-token 请求头）
     *
     * @param request
     * @return
     */
    public static String getTokenFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(token)) {
            if (token.startsWith("Bearer ")) {
                token = token.substring("Bearer ".length());
            }
            return token;
        }
        return request.getHeader(GatewayConsts.HEADER_TOKEN);
    }

    /**
     * 从 request 中获取登录用户 id（优先取拦截器放入的 attribute，回退网关透传的请求头）
     *
     * @param request
     * @return 未登录时返回 null
     */
    public static Long getLoginUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(LOGIN_USER_ID_ATTRIBUTE);
        if (userId == null) {
            String headerUserId = request.getHeader(GatewayConsts.HEADER_USER_ID);
            return StringUtils.isBlank(headerUserId) ? null : Long.valueOf(headerUserId);
        }
        return userId instanceof Long ? (Long) userId : Long.valueOf(userId.toString());
    }
}
