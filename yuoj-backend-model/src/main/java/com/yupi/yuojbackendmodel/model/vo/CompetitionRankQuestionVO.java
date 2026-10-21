package com.yupi.yuojbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 竞赛排名中单题情况视图
 */
@Data
public class CompetitionRankQuestionVO implements Serializable {

    /**
     * 提交次数
     */
    private Integer submitCount;

    /**
     * 是否通过
     */
    private Boolean accepted;

    /**
     * 通过用时（秒），未通过为 -1
     */
    private Long acceptTimeSeconds;

    /**
     * 冻结提交次数
     */
    private Integer frozenSubmitCount;

    /**
     * 赛后通过用时（秒）：赛后首次通过时间 - 比赛开始时间，赛后未通过为 -1
     */
    private Long postContestAcceptSeconds;

    /**
     * 赛后是否通过
     */
    private Boolean postContestAccepted;

    /**
     * 是否一血（第一个通过）
     */
    private Boolean firstBlood;

    private static final long serialVersionUID = 1L;
}
