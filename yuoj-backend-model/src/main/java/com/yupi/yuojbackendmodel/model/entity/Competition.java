package com.yupi.yuojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.List;

@Data
public class Competition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String competitionTitle;

    private String description;

    private String startTime;

    private String endTime;

    private String status;
}
