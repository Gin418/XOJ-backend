package com.oj.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 竞赛排名条目视图
 */
@Data
public class CompetitionRankItemVO implements Serializable {

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 排名
     */
    private Integer rank;

    /**
     * 通过题目数
     */
    private Integer acceptedCount;

    /**
     * 罚时（秒）：首次通过前的失败提交数 × 20 分钟 + 通过记录与竞赛开始的秒差
     */
    private Long penaltyTime;

    /**
     * 每题情况，key 为题目 id（按题号顺序）
     */
    private Map<Long, CompetitionRankQuestionVO> questions;

    /**
     * 用户信息（脱敏）
     */
    private UserVO userVO;

    private static final long serialVersionUID = 1L;
}
