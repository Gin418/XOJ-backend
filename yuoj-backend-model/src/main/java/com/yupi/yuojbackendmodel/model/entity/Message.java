package com.yupi.yuojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 消息（系统通知 / 私信）
 *
 * @author Q
 */
@TableName(value = "message")
@Data
public class Message implements Serializable {

    /**
     * 未读
     */
    public static final Integer UNREAD = 0;

    /**
     * 已读
     */
    public static final Integer READ = 1;

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
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
     * 接收用户 id（广播通知为 null，不落库）
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

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
