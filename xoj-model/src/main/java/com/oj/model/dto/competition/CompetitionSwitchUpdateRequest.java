package com.oj.model.dto.competition;

import lombok.Data;

/**
 * 竞赛开关更新请求（是否允许赛后提交、是否揭榜）
 */
@Data
public class CompetitionSwitchUpdateRequest {

    /**
     * 竞赛 id
     */
    private Long id;

    /**
     * 是否允许赛后提交（0 不允许、1 允许）
     */
    private Integer allowSubmitAfterEnd;

    /**
     * 是否揭榜（0 未揭榜、1 已揭榜）
     */
    private Integer isRevealed;
}
