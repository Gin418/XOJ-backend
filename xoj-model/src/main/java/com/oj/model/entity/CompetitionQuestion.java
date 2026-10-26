package com.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 竞赛题目关系
 *
 * @TableName competition_question
 */
@TableName(value = "competition_question")
@Data
public class CompetitionQuestion implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 竞赛 id
     */
    private Long competitionId;

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 题目顺序（展示题号：A、B、C、D……）
     */
    private Integer questionOrder;

    /**
     * 提交数
     */
    private Integer submitNum;

    /**
     * 通过数
     */
    private Integer acceptedNum;

    /**
     * 第一个通过的用户 id
     */
    private Long firstAcceptedUserId;

    /**
     * 首个通过用户的提交时间
     */
    private Date firstAcceptedTime;

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
