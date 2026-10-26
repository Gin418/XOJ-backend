package com.oj.common.config;

import com.oj.common.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * @Author Q
 * @Description Spring MVC 配置：注册登录态校验拦截器，被 @ComponentScan("com.oj") 的服务自动生效
 * @Date 2026/10/11 20:30
 * @Version 1.0.0
 **/
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 免登录访问的路径白名单（相对各服务的 context-path 匹配）
     */
    private static final String[] EXCLUDE_PATHS = {
            // 用户服务：登录、注册
            "/register",
            "/login",
            // 题目服务：公开查询接口
            "/get/vo",
            "/list/page/vo",
            "/question_submit/list/page",
            "/count/**",
            "/stats/**",
            "/language/select",
            // 文件上传（沿用现状公开访问）
            "/file/upload",
            // 服务内部调用接口
            "/inner/**"
    };

    @Resource
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**");
    }
}
