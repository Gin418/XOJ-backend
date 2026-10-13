package com.yupi.yuojbackendcommon.interceptor;

import com.google.gson.Gson;
import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.common.ResultUtils;
import com.yupi.yuojbackendcommon.constant.GatewayHeaderConsts;
import com.yupi.yuojbackendcommon.constant.GatewayHeaderContext;
import com.yupi.yuojbackendcommon.utils.GatewayHeaderUtil;
import com.yupi.yuojbackendcommon.utils.LoginUserUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author Q
 * @Description 登录态校验拦截器：未登录请求直接返回 401，白名单路径由 WebMvcConfig 放行
 * @Date 2026/10/11 20:30
 * @Version 1.0.0
 **/
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final Gson GSON = new Gson();

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String ucToken = request.getHeader(GatewayHeaderConsts.HEADER_TOKEN);
        String traceId = request.getHeader(GatewayHeaderConsts.HEADER_TRACE_ID);
        Long userId = null;
        if (request.getHeader(GatewayHeaderConsts.HEADER_USER_ID) != null) {
            userId = Long.valueOf(request.getHeader(GatewayHeaderConsts.HEADER_USER_ID));
        }

        String userName = null;
        if (request.getHeader(GatewayHeaderConsts.HEADER_USER_NAME) != null) {
            userName = request.getHeader(GatewayHeaderConsts.HEADER_USER_NAME);
        }

        String userRole = null;
        if (request.getHeader(GatewayHeaderConsts.HEADER_USER_ROLE) != null) {
            userRole = request.getHeader(GatewayHeaderConsts.HEADER_USER_ROLE);
        }

        GatewayHeaderContext gatewayHeaderContext = GatewayHeaderContext.builder()
                .UC_TOKEN(ucToken)
                .TRACE_ID(traceId)
                .X_USER_ID(userId)
                .X_USER_NAME(userName)
                .X_USER_ROLE(userRole)
                .build();
        GatewayHeaderUtil.threadLocal.set(gatewayHeaderContext);

        return HandlerInterceptor.super.preHandle(request, response, handler);
    }
}
