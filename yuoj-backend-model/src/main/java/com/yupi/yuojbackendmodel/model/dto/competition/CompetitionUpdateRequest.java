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
     * 题目Ids
     */
    private List<Long> questionIds;
}
