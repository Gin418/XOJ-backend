package com.yupi.yuojbackendcommon.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * JwtUtil 签名算法执行演示
 */
public class JwtUtilTest {

    @Test
    public void testGenerateToken() {
        // 1. 生成 token
        String token = JwtUtil.generateToken(1L, "user");
        System.out.println("生成的 token：" + token);
        // 2. 验签（签名是否正确且未过期）
        boolean verify = JwtUtil.verifyToken(token);
        System.out.println("验签结果：" + verify);
        Assertions.assertTrue(verify);
        // 3. 解析 userId
        Long userId = JwtUtil.getUserId(token);
        System.out.println("解析出的 userId：" + userId);
        Assertions.assertEquals(1L, userId);
    }

    @Test
    public void testVerifyWrongToken() {
        // 篡改 payload 后验签应失败
        String token = JwtUtil.generateToken(1L, "user");
        String tamperedToken = token.substring(0, token.length() - 2) + "xx";
        boolean verify = JwtUtil.verifyToken(tamperedToken);
        System.out.println("被篡改 token 的验签结果：" + verify);
        Assertions.assertFalse(verify);
    }
}
