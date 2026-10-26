package com.oj.userservice.rabbitmq;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.oj.model.vo.MessageVO;
import com.oj.userservice.sse.SseEmitterManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Type;
import java.util.List;

/**
 * 消息消费者：监听广播队列，将消息列表中的每条消息推送给本实例上连接的对应 SSE 客户端。
 * receiverId 为空表示广播通知，推送给所有在线用户。
 */
@Component
@Slf4j
public class MessageMqConsumer {

    private static final Gson GSON = new Gson();

    private static final Type MESSAGE_VO_LIST_TYPE = new TypeToken<List<MessageVO>>() {
    }.getType();

    @Resource
    private SseEmitterManager sseEmitterManager;

    @RabbitListener(queues = "#{messageQueue.name}")
    public void onMessage(String messageJson) {
        List<MessageVO> messageVOList = GSON.fromJson(messageJson, MESSAGE_VO_LIST_TYPE);
        if (messageVOList == null || messageVOList.isEmpty()) {
            log.warn("收到空消息，忽略");
            return;
        }
        for (MessageVO messageVO : messageVOList) {
            if (messageVO.getReceiverId() == null) {
                // 广播通知：推送给所有在线用户
                sseEmitterManager.sendToAll(SseEmitterManager.EVENT_MESSAGE, messageVO);
            } else {
                // 私信 / 指定用户通知：推送给接收方在本实例上的连接
                sseEmitterManager.sendToUser(messageVO.getReceiverId(), SseEmitterManager.EVENT_MESSAGE, messageVO);
            }
        }
    }
}
