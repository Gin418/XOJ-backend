package com.oj.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 竞赛题目视图（含题号与竞赛内统计）
 */
@Data
public class CompetitionQuestionVO {

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 题目顺序（1=A，2=B……）
     */
    private Integer questionOrder;

    /**
     * 题号（A、B、C……）
     */
    private String customId;

    /**
     * 题目标题
     */
    private String title;

    /**
     * 当前用户提交情况（0 未提交、1 提交未通过、2 通过，取值 QuestionStatusEnum）
     */
    private Integer questionStatus;

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
     * 首杀用户信息（脱敏）
     */
    private UserVO firstAcceptedUserVO;
}
