package com.yupi.yuojbackendmodel.model.dto.competition;

import lombok.Data;

import java.util.List;

@Data
public class CompetitionUpdateRequest {

    private Long id;

    /**
     * 竞赛标题
     */
    private String competitionTitle;

    /**
     * 描述
     */
    private String description;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 封榜时间（封榜后排名不再更新）
     */
    private String freezeTime;

    /**
     * 是否允许赛后提交（0 不允许、1 允许）
     */
    private Integer allowSubmitAfterEnd;

    /**
     * 是否揭榜（0 未揭榜、1 已揭榜）
     */
    private Integer isRevealed;

    /**
     * 题目Ids
     */
    private List<Long> questionIds;
}
