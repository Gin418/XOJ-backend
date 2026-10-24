package com.yupi.yuojbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 消息视图（脱敏，含发送者信息）
 *
 * @author Q
 */
@Data
public class MessageVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 消息类型：0 系统通知、1 私信
     */
    private Integer type;

    /**
     * 标题（系统通知使用）
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 发送用户 id（私信使用，系统通知为 null）
     */
    private Long senderId;

    /**
     * 发送用户昵称
     */
    private String senderName;

    /**
     * 发送用户头像
     */
    private String senderAvatar;

    /**
     * 接收用户 id（广播通知为 null）
     */
    private Long receiverId;

    /**
     * 是否已读：0 未读、1 已读
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
