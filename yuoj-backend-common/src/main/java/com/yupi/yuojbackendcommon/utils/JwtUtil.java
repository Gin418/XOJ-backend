package com.yupi.yuojbackendcommon.utils;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * @Author Q
 * @Description JWT 工具类：登录 token 的生成、校验与解析
 * @Date 2026/10/11 17:12
 * @Version 1.0.0
 **/
public class JwtUtil {

    /**
     * JWT 签名密钥
     */
    private static final String SECRET = "gin@&#=asd!418";

    /**
     * token 过期时间：30 天（毫秒），与 session 超时保持一致
     */
    private static final long EXPIRE_TIME = 30L * 24 * 60 * 60 * 1000;

    /**
     * 用户登录时生成 token
     *
     * @param userId   用户 id
     * @param userRole 用户角色
     * @return
     */
    public static String generateToken(long userId, String userRole) {
        Date now = new Date();
        return JWT.create()
                .setPayload("userId", String.valueOf(userId))
                .setPayload("userRole", userRole)
                .setIssuedAt(now)
                .setExpiresAt(new Date(now.getTime() + EXPIRE_TIME))
                .setKey(SECRET.getBytes(StandardCharsets.UTF_8))
                .sign();
    }

    /**
     * 校验 token 是否有效（签名正确且未过期）
     *
     * @param token
     * @return
     */
    public static boolean verifyToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        try {
            JWT jwt = JWTUtil.parseToken(token);
            if (!jwt.setKey(SECRET.getBytes(StandardCharsets.UTF_8)).verify()) {
                return false;
            }
            JWTValidator.of(token).validateDate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 token 中解析用户 id
     *
     * @param token
     * @return
     */
    public static Long getUserId(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        jwt.setKey(SECRET.getBytes(StandardCharsets.UTF_8));
        Object userId = jwt.getPayload("userId");
        return userId == null ? null : Long.valueOf(userId.toString());
    }
}
