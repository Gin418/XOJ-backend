package com.yupi.yuojbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 竞赛排名页题目统计（题号、标题、提交数、通过数）
 */
@Data
public class CompetitionRankQuestionInfoVO implements Serializable {

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 题号（A、B、C……）
     */
    private String customId;

    /**
     * 题目标题
     */
    private String title;

    /**
     * 提交数
     */
    private Integer submitNum;

    /**
     * 通过数
     */
    private Integer acceptedNum;

    private static final long serialVersionUID = 1L;
}
