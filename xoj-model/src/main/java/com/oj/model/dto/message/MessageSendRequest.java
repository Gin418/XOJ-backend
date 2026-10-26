package com.oj.model.dto.message;

import lombok.Data;

import java.io.Serializable;

/**
 * 私信发送请求
 *
 * @author Q
 */
@Data
public class MessageSendRequest implements Serializable {

    /**
     * 接收用户 id
     */
    private Long receiverId;

    /**
     * 消息内容
     */
    private String content;

    private static final long serialVersionUID = 1L;
}
