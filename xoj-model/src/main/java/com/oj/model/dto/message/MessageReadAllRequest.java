package com.oj.model.dto.message;

import lombok.Data;

import java.io.Serializable;

/**
 * 消息全部已读请求
 *
 * @author Q
 */
@Data
public class MessageReadAllRequest implements Serializable {

    /**
     * 消息类型：0 系统通知、1 私信
     */
    private Integer type;

    /**
     * 好友用户 id（私信会话全部已读使用）
     */
    private Long friendId;

    private static final long serialVersionUID = 1L;
}
