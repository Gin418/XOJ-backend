package com.yupi.yuojbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 私信会话视图（会话列表项）
 *
 * @author Q
 */
@Data
public class ConversationVO implements Serializable {

    /**
     * 好友用户 id
     */
    private Long friendId;

    /**
     * 好友昵称
     */
    private String friendName;

    /**
     * 好友头像
     */
    private String friendAvatar;

    /**
     * 最后一条消息内容
     */
    private String lastContent;

    /**
     * 最后一条消息时间
     */
    private Date lastTime;

    /**
     * 未读消息数量（对方发给我的未读私信）
     */
    private long unreadCount;

    /**
     * 第一条未读消息 id（对方发给我的未读私信中最早的一条，无未读为 null）
     */
    private Long firstUnreadId;

    /**
     * 最后一条消息是否是我发的
     */
    private Boolean lastMessageIsMine;

    private static final long serialVersionUID = 1L;
}
