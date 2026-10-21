package com.yupi.yuojbackendmodel.model.vo;

import lombok.Data;

/**
 * @Author Q
 * @Description 最近 N 天每日提交统计视图对象
 * @Date 2026/10/20
 * @Version 1.0.0
 **/
@Data
public class DailySubmitStatsVO {

    /**
     * 日期（yyyy-MM-dd）
     */
    private String date;

    /**
     * 提交次数
     */
    private Long submitCount;

    /**
     * AC 次数
     */
    private Long acceptedCount;
}
