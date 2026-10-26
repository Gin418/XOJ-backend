package com.oj.userservice.sse;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE 连接管理器：维护 用户 id -> 该用户所有 SSE 连接（支持多端登录）的映射，
 * 负责连接注册、单用户推送、全员广播以及心跳保活。
 *
 * @author Q
 */
@Component
@Slf4j
public class SseEmitterManager {

    /**
     * SSE 事件名：消息推送（私信 / 系统通知）
     */
    public static final String EVENT_MESSAGE = "message";

    /**
     * SSE 事件名：连接建立回执（前端可据此确认已连上）
     */
    public static final String EVENT_CONNECTED = "connected";

    /**
     * 连接超时时间（毫秒）：0 表示永不超时（Servlet 规范）
     */
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    /**
     * 心跳间隔（毫秒）
     */
    private static final long HEARTBEAT_INTERVAL = 2_000L;

    private static final Gson GSON = new Gson();

    /**
     * 用户 id -> 该用户的所有 SSE 连接
     */
    private final Map<Long, List<SseEmitter>> USER_EMITTERS = new ConcurrentHashMap<>();

    /**
     * 为用户创建一条 SSE 连接并注册到连接表
     *
     * @param userId
     * @return
     */
    public SseEmitter createEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        List<SseEmitter> emitters = USER_EMITTERS.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);
        Runnable cleanUp = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(cleanUp);
        emitter.onTimeout(cleanUp);
        emitter.onError(e -> cleanUp.run());
        // 连接建立回执
        sendEvent(emitter, EVENT_CONNECTED, userId);
        log.info("SSE 连接建立，userId = {}", userId);
        return emitter;
    }

    /**
     * 向指定用户的所有连接推送消息
     *
     * @param userId
     * @param eventName
     * @param data
     */
    public void sendToUser(Long userId, String eventName, Object data) {
        List<SseEmitter> emitters = USER_EMITTERS.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            boolean success = sendEvent(emitter, eventName, data);
            if (!success) {
                removeEmitter(userId, emitter);
            }
        }
    }

    /**
     * 向所有在线用户广播消息
     *
     * @param eventName
     * @param data
     */
    public void sendToAll(String eventName, Object data) {
        USER_EMITTERS.forEach((userId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                boolean success = sendEvent(emitter, eventName, data);
                if (!success) {
                    removeEmitter(userId, emitter);
                }
            }
        });
    }

    /**
     * 心跳保活：定时发送 SSE 注释行，防止网关 / 反向代理空闲超时断开连接
     */
    @Scheduled(fixedDelay = HEARTBEAT_INTERVAL)
    public void sendHeartbeat() {
        USER_EMITTERS.forEach((userId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    removeEmitter(userId, emitter);
                }
            }
        });
    }

    /**
     * 向单个连接发送事件
     *
     * @param emitter
     * @param eventName
     * @param data
     * @return 发送成功返回 true，失败返回 false
     */
    private boolean sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(GSON.toJson(data)));
            return true;
        } catch (Exception e) {
            log.warn("SSE 消息发送失败，eventName = {}", eventName, e);
            return false;
        }
    }

    /**
     * 移除连接（用户连接全部断开后清理映射）
     *
     * @param userId
     * @param emitter
     */
    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = USER_EMITTERS.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // 连接已关闭时忽略
        }
        if (emitters.isEmpty()) {
            USER_EMITTERS.remove(userId);
            log.info("SSE 连接全部断开，userId = {}", userId);
        }
    }
}
