package com.yupi.yuojbackenduserservice.rabbitmq;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 消息广播交换机配置：
 * 声明式创建 Fanout 交换机，每个实例绑定一个匿名独占队列，
 * 实现消息广播到所有实例（各实例只推送给本地连接的 SSE 客户端）。
 */
@Configuration
public class MessageRabbitMqConfig {

    /**
     * 消息广播交换机名称
     */
    public static final String MESSAGE_EXCHANGE = "message_exchange";

    // 声明持久化广播交换机
    @Bean
    public FanoutExchange messageFanoutExchange() {
        return new FanoutExchange(MESSAGE_EXCHANGE, true, false);
    }

    // 每个实例一个匿名队列（非持久化、独占、自动删除）
    @Bean
    public AnonymousQueue messageQueue() {
        return new AnonymousQueue();
    }

    // 将匿名队列绑定到广播交换机
    @Bean
    public Binding messageQueueBinding() {
        return BindingBuilder.bind(messageQueue()).to(messageFanoutExchange());
    }
}
