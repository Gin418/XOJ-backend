package com.oj.userservice.controller;

import com.oj.common.common.ErrorCode;
import com.oj.common.exception.ThrowUtils;
import com.oj.common.utils.LoginUserUtil;
import com.oj.model.entity.User;
import com.oj.userservice.service.UserService;
import com.oj.userservice.sse.SseEmitterManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * SSE 长连接接口：消息通知与私信实时推送
 *
 * @author Q
 */
@RestController
@RequestMapping("/sse")
@Slf4j
public class SseController {

    @Resource
    private SseEmitterManager sseEmitterManager;

    @Resource
    private UserService userService;

    /**
     * 建立 SSE 连接
     *
     * @param request
     * @return
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(HttpServletRequest request) {
        String token = LoginUserUtil.getTokenFromRequest(request);
        User loginUser = userService.getLoginUserByToken(token);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "登录校验已过期，需重新登录");
        return sseEmitterManager.createEmitter(loginUser.getId());
    }

}
