package com.yupi.yuojbackendjudgeservice.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 用于创建测试程序用到的交换机和队列（只用在程序启动前执行一次）
 * 声明式创建：将 Queue、Exchange、Binding 注册为 Bean，
 * 由 RabbitAdmin 在应用启动时自动声明到 Broker（与监听器使用同一连接配置）
 */
@Configuration
public class InitRabbitMqBean {

    // 交换机名称
    private static final String CODE_EXCHANGE_NAME = "code_exchange";

    // 队列名称
    private static final String CODE_QUEUE_NAME = "code_queue";

    // 路由键
    private static final String CODE_ROUTING_KEY = "my_routingKey";

    // 声明持久化队列
    @Bean
    public Queue codeQueue() {
        return new Queue(CODE_QUEUE_NAME, true);
    }

    // 声明持久化交换机
    @Bean
    public DirectExchange codeExchange() {
        return new DirectExchange(CODE_EXCHANGE_NAME, true, false);
    }

    // 将队列绑定到交换机
    @Bean
    public Binding codeQueueBinding() {
        return BindingBuilder.bind(codeQueue()).to(codeExchange()).with(CODE_ROUTING_KEY);
    }
}
