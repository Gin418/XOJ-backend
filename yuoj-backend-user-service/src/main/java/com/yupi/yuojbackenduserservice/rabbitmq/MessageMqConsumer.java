package com.yupi.yuojbackenduserservice.rabbitmq;

import com.google.gson.Gson;
import com.yupi.yuojbackendmodel.model.vo.MessageVO;
import com.yupi.yuojbackenduserservice.sse.SseEmitterManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 消息消费者：监听广播队列，将消息推送给本实例上连接的对应 SSE 客户端。
 * receiverId 为空表示广播通知，推送给所有在线用户。
 */
@Component
@Slf4j
public class MessageMqConsumer {

    private static final Gson GSON = new Gson();

    @Resource
    private SseEmitterManager sseEmitterManager;

    @RabbitListener(queues = "#{messageQueue.name}")
    public void onMessage(String messageJson) {
        MessageVO messageVO = GSON.fromJson(messageJson, MessageVO.class);
        if (messageVO == null) {
            log.warn("收到空消息，忽略");
            return;
        }
        if (messageVO.getReceiverId() == null) {
            // 广播通知：推送给所有在线用户
            sseEmitterManager.sendToAll(SseEmitterManager.EVENT_MESSAGE, messageVO);
        } else {
            // 私信 / 指定用户通知：推送给接收方在本实例上的连接
            sseEmitterManager.sendToUser(messageVO.getReceiverId(), SseEmitterManager.EVENT_MESSAGE, messageVO);
        }
    }
}
