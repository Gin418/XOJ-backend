package com.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.List;

@Data
public class Competition {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String competitionTitle;

    private String description;

    private String startTime;

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

    private Long userId;

    private String status;
}
