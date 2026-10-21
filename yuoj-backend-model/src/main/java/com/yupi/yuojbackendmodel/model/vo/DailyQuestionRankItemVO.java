package com.yupi.yuojbackendmodel.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * @Author Q
 * @Description 每日一题解题榜条目视图对象
 * @Date 2026/10/20
 * @Version 1.0.0
 **/
@Data
public class DailyQuestionRankItemVO {

    /**
     * 排名（1 为最早通过）
     */
    private Integer rank;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 用户信息（脱敏）
     */
    private UserVO userVO;

    /**
     * 首次通过时间
     */
    private Date acceptedTime;
}
