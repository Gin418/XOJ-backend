package com.yupi.yuojbackendmodel.model.dto.competition;

import com.yupi.yuojbackendcommon.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 竞赛排名分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CompetitionRankQueryRequest extends PageRequest {

    /**
     * 是否实时榜单（true 查全部提交、false 正式榜单只查比赛期间的提交）
     */
    private Boolean realTime = false;
}
