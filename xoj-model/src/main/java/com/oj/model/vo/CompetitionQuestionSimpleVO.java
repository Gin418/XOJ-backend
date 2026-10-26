package com.oj.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 竞赛题目精简视图（仅题号、题目标题、题目 id）
 */
@Data
public class CompetitionQuestionSimpleVO implements Serializable {

    /**
     * 题目 id
     */
    private Long questionId;

    private String label;

    private static final long serialVersionUID = 1L;
}
