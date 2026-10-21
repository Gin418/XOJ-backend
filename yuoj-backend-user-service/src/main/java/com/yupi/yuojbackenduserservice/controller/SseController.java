package com.yupi.yuojbackenduserservice.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @Author Q
 * @Description TODO
 * @Date 2026/10/21 23:08
 * @Version 1.0.0
 **/
@RestController
@RequestMapping("/sse")
public class SseController {
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sse() {
        return new SseEmitter();
    }

}
