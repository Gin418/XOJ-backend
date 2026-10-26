package com.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 私信会话（每个用户与一位好友一条会话记录，发送私信或打开会话时创建）
 *
 * @author Q
 */
@TableName(value = "conversation")
@Data
public class Conversation implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 会话所属用户 id
     */
    private Long userId;

    /**
     * 对方用户 id
     */
    private Long friendId;

    /**
     * 最后一条消息 id（冗余，发送私信时更新）
     */
    private Long lastMessageId;

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
