package com.oj.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 竞赛题目详情（竞赛内题号、统计 + 题目详情）
 */
@Data
public class CompetitionQuestionDetailVO {

    /**
     * 题目顺序（1=A，2=B……）
     */
    private Integer questionOrder;

    /**
     * 题号（A、B、C……）
     */
    private String customId;

    /**
     * 竞赛内提交数
     */
    private Integer submitNum;

    /**
     * 竞赛内通过数
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
     * 题目详情（脱敏）
     */
    private QuestionVO questionVO;
}
