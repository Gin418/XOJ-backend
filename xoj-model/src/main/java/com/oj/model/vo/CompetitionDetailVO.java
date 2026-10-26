package com.oj.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 竞赛详情（竞赛全部信息 + 关联题目 id 列表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CompetitionDetailVO extends CompetitionVO {

    /**
     * 创建人用户 id
     */
    private Long userId;

    /**
     * 关联题目 id 列表（按题号顺序 A、B、C……）
     */
    private List<Long> questionIds;
}
