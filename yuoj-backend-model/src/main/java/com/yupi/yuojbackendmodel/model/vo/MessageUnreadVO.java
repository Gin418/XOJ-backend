package com.yupi.yuojbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 未读消息数量视图（按消息类型拆分）
 *
 * @author Q
 */
@Data
public class MessageUnreadVO implements Serializable {

    /**
     * 未读系统通知数量
     */
    private long systemUnreadCount;

    /**
     * 未读私信数量
     */
    private long privateUnreadCount;

    private static final long serialVersionUID = 1L;
}
