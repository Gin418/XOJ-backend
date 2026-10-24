package com.yupi.yuojbackenduserservice.rabbitmq;

import com.google.gson.Gson;
import com.yupi.yuojbackendmodel.model.vo.MessageVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 消息生产者：将消息 VO 发布到广播交换机，由各实例的消费者推送给本地 SSE 连接
 */
@Component
public class MessageMqProducer {

    private static final Gson GSON = new Gson();

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发布消息（receiverId 为空表示广播给所有在线用户）
     *
     * @param messageVO
     */
    public void publish(MessageVO messageVO) {
        rabbitTemplate.convertAndSend(MessageRabbitMqConfig.MESSAGE_EXCHANGE, "", GSON.toJson(messageVO));
    }
}
