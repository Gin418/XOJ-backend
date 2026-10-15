package com.yupi.yuojbackendgateway.filter;

import com.google.gson.Gson;
import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.common.ResultUtils;
import com.yupi.yuojbackendcommon.constant.GatewayConsts;
import com.yupi.yuojbackendcommon.utils.LoginUserUtil;
import com.yupi.yuojbackendcommon.utils.Tools;
import com.yupi.yuojbackendmodel.model.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class GlobalAuthFilter implements GlobalFilter, Ordered {

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    private static final Gson GSON = new Gson();

    /**
     * 免登录放行的路径白名单（含网关路由前缀）
     */
    private static final String[] WHITE_LIST = {
            // 用户服务：登录、注册、头像上传
            "/api/user/register",
            "/api/user/login",
            "/api/user/file/upload",
            // 题目服务：公开查询接口
            "/api/question/get/vo",
            "/api/question/list/page/vo",
            "/api/question/simple/list",
            "/api/question/question_submit/list/page",
            "/api/question/count/**",
            "/api/question/stats/**",
            "/api/question/language/select",
            "/api/question/file/upload",
            // 接口文档
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    };

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        String path = serverHttpRequest.getURI().getPath();
        Map<String, String> headers = Tools.getHeaderMap(serverHttpRequest.getHeaders());
        String token = headers.get(GatewayConsts.HEADER_TOKEN);
        String traceId = StringUtils.defaultIfBlank(headers.get(GatewayConsts.HEADER_TRACE_ID), Tools.generateUuid());
        // 判断路径中是否包含 inner，只允许内部调用
        if (antPathMatcher.match("/**/inner/**", path)) {
            return writeJsonResponse(exchange, HttpStatus.FORBIDDEN, "无权限");
        }
        // 通过 token 从 Redis 中获取登录用户信息
        User loginUser = LoginUserUtil.getLoginUser(stringRedisTemplate, token, User.class);
        Long userId = loginUser != null ? loginUser.getId() : 999;
        String userName = loginUser != null ? loginUser.getUserName() : null;
        String userRole = loginUser != null ? loginUser.getUserRole() : null;
        // 将用户信息透传给下游服务
        serverHttpRequest = serverHttpRequest.mutate()
                .header(GatewayConsts.HEADER_USER_ID, String.valueOf(userId))
                .header(GatewayConsts.HEADER_USER_NAME, userName)
                .header(GatewayConsts.HEADER_USER_ROLE, userRole)
                .header(GatewayConsts.HEADER_TRACE_ID, traceId)
                .build();

        // 免登录白名单直接放行
        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }

        if (loginUser == null) {
            // 登录校验已过期，需重新登录
            return writeJsonResponse(exchange, HttpStatus.UNAUTHORIZED,
                    GSON.toJson(ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, "登录校验已过期，需重新登录")));
        }
        return chain.filter(exchange.mutate().request(serverHttpRequest).build());
    }

    /**
     * 判断是否为免登录白名单路径
     *
     * @param path
     * @return
     */
    private boolean isWhiteList(String path) {
        for (String pattern : WHITE_LIST) {
            if (antPathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回 JSON 响应并结束请求
     *
     * @param exchange
     * @param status
     * @param body
     * @return
     */
    private Mono<Void> writeJsonResponse(ServerWebExchange exchange, HttpStatus status, String body) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer dataBuffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(dataBuffer));
    }

    /**
     * 优先级提到最高
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
