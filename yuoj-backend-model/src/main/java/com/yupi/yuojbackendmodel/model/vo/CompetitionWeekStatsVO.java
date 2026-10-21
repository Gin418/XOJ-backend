package com.yupi.yuojbackendmodel.model.vo;

import lombok.Data;

/**
 * 本周竞赛统计 VO
 */
@Data
public class CompetitionWeekStatsVO {

    /**
     * 本周竞赛总数
     */
    private Integer allCount;

    /**
     * 未开始数量
     */
    private Integer pendingCount;

    /**
     * 进行中数量
     */
    private Integer ongoingCount;
}
