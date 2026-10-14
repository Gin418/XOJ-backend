package com.yupi.yuojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户竞赛关系
 *
 * @TableName user_competition
 */
@TableName(value = "user_competition")
@Data
public class UserCompetition implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 竞赛 id
     */
    private Long competitionId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 罚时
     */
    private Long penaltyTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
