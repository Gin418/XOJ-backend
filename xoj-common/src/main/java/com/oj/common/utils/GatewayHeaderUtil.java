package com.oj.common.utils;

import com.oj.common.constant.GatewayHeaderContext;

import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * @Author Q
 * @Description 网关透传用户信息上下文工具类：
 * 拦截器将网关透传的用户信息（token、用户 id 等）写入 ThreadLocal，
 * 业务代码可直接通过本类读取当前请求的登录用户信息；
 * 使用线程池开启子线程时，需用 wrap 方法包裹任务，将父线程上下文传递给子线程。
 * @Date 2026/10/11 21:30
 * @Version 1.0.0
 **/
public class GatewayHeaderUtil {

    /**
     * 用户信息上下文，LoginInterceptor 中直接通过 threadLocal.set 写入
     */
    public static final ThreadLocal<GatewayHeaderContext> threadLocal = new ThreadLocal<>();

    /**
     * 写入当前线程的用户信息上下文
     *
     * @param context
     */
    public static void set(GatewayHeaderContext context) {
        threadLocal.set(context);
    }

    /**
     * 获取当前线程的用户信息上下文
     *
     * @return 未登录或未经过拦截器时返回 null
     */
    public static GatewayHeaderContext getContext() {
        return Optional.ofNullable(threadLocal.get()).orElse(null);
    }

    /**
     * 获取当前登录用户 id
     *
     * @return 未登录时返回 0
     */
    public static Long getUserId() {
        return Optional.ofNullable(threadLocal.get())
                .map(gatewayHeaderContext -> gatewayHeaderContext.getX_USER_ID())
                .orElse(0L);
    }

    /**
     * 获取当前登录用户昵称
     *
     * @return 未登录时返回空字符串
     */
    public static String getUserName() {
        return Optional.ofNullable(threadLocal.get())
                .map(gatewayHeaderContext -> gatewayHeaderContext.getX_USER_NAME())
                .orElse("");
    }

    /**
     * 获取当前登录用户角色
     *
     * @return 未登录时返回空字符串
     */
    public static String getUserRole() {
        return Optional.ofNullable(threadLocal.get())
                .map(gatewayHeaderContext -> gatewayHeaderContext.getX_USER_ROLE())
                .orElse("");
    }

    /**
     * 获取当前请求的 token
     *
     * @return 未登录时返回空字符串
     */
    public static String getToken() {
        return Optional.ofNullable(threadLocal.get())
                .map(gatewayHeaderContext -> gatewayHeaderContext.getUC_TOKEN())
                .orElse("");
    }

    /**
     * 获取当前请求的链路追踪 id
     *
     * @return 无 traceId 时返回空字符串
     */
    public static String getTraceId() {
        return Optional.ofNullable(threadLocal.get())
                .map(gatewayHeaderContext -> gatewayHeaderContext.getTRACE_ID())
                .orElse("");
    }

    /**
     * 单独设置当前线程上下文的用户 id，其余字段保持不变。
     * 采用复制语义：子线程中调用不会影响父线程的上下文。
     *
     * @param userId
     */
    public static void setUserId(Long userId) {
        GatewayHeaderContext newContext = copyOf(threadLocal.get());
        newContext.setX_USER_ID(userId);
        threadLocal.set(newContext);
    }

    /**
     * 单独设置当前线程上下文的用户昵称，其余字段保持不变。
     *
     * @param userName
     */
    public static void setUserName(String userName) {
        GatewayHeaderContext newContext = copyOf(threadLocal.get());
        newContext.setX_USER_NAME(userName);
        threadLocal.set(newContext);
    }

    /**
     * 单独设置当前线程上下文的用户角色，其余字段保持不变。
     *
     * @param userRole
     */
    public static void setUserRole(String userRole) {
        GatewayHeaderContext newContext = copyOf(threadLocal.get());
        newContext.setX_USER_ROLE(userRole);
        threadLocal.set(newContext);
    }

    /**
     * 单独设置当前线程上下文的 token，其余字段保持不变。
     *
     * @param token
     */
    public static void setToken(String token) {
        GatewayHeaderContext newContext = copyOf(threadLocal.get());
        newContext.setUC_TOKEN(token);
        threadLocal.set(newContext);
    }

    /**
     * 清除当前线程的上下文，请求结束后必须调用，防止内存泄漏
     */
    public static void remove() {
        threadLocal.remove();
    }

    /**
     * 多线程场景：将父线程的用户信息上下文传递给子线程执行 Runnable 任务。
     * 线程池中的线程不会自动继承 ThreadLocal 值，提交任务前必须用本方法包裹。
     *
     * @param task
     * @return 父线程无上下文时原样返回任务，否则返回带上下文传递能力的包装任务
     */
    public static Runnable wrap(Runnable task) {
        final GatewayHeaderContext context = threadLocal.get();
        if (context == null) {
            return task;
        }
        return () -> {
            threadLocal.set(context);
            try {
                task.run();
            } finally {
                threadLocal.remove();
            }
        };
    }

    /**
     * 多线程场景：将父线程的用户信息上下文传递给子线程执行 Callable 任务。
     *
     * @param task
     * @return 父线程无上下文时原样返回任务，否则返回带上下文传递能力的包装任务
     */
    public static <T> Callable<T> wrap(Callable<T> task) {
        final GatewayHeaderContext context = threadLocal.get();
        if (context == null) {
            return task;
        }
        return () -> {
            threadLocal.set(context);
            try {
                return task.call();
            } finally {
                threadLocal.remove();
            }
        };
    }

    /**
     * 复制上下文对象：为 null 时返回空上下文，否则基于原值构建新对象，
     * 保证单字段 set 不影响其他线程持有的同一上下文引用
     *
     * @param context
     * @return
     */
    private static GatewayHeaderContext copyOf(GatewayHeaderContext context) {
        if (context == null) {
            return GatewayHeaderContext.builder().build();
        }
        return GatewayHeaderContext.builder()
                .UC_TOKEN(context.getUC_TOKEN())
                .TRACE_ID(context.getTRACE_ID())
                .SPAN_ID(context.getSPAN_ID())
                .X_USER_ID(context.getX_USER_ID())
                .X_USER_NAME(context.getX_USER_NAME())
                .X_USER_ROLE(context.getX_USER_ROLE())
                .X_HOTEL_ID(context.getX_HOTEL_ID())
                .X_HOTEL_CODE(context.getX_HOTEL_CODE())
                .X_HOTEL_GROUP_ID(context.getX_HOTEL_GROUP_ID())
                .X_HOTEL_GROUP_CODE(context.getX_HOTEL_GROUP_CODE())
                .X_CHANNEL_CODE(context.getX_CHANNEL_CODE())
                .X_CLIENT_IP(context.getX_CLIENT_IP())
                .X_VALUE(context.getX_VALUE())
                .build();
    }
}
