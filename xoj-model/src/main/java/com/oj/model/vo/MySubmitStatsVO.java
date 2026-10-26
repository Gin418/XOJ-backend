package com.oj.model.vo;

import lombok.Data;

/**
 * @Author Q
 * @Description 我的提交统计视图对象
 * @Date 2026/9/30 15:13
 * @Version 1.0.0
 **/
@Data
public class MySubmitStatsVO {

    /**
     * 今日提交数
     */
    private Long todaySubmitCount;

    /**
     * 本月提交数
     */
    private Long monthSubmitCount;

    /**
     * 连续提交天数
     */
    private Long continuousSubmitDays;

    /**
     * 本月通过数
     */
    private Long monthAcceptedCount;
}
