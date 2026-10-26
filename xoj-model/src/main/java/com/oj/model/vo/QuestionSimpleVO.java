package com.oj.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 题目简信息（题目 id、题号、标题）
 */
@Data
public class QuestionSimpleVO implements Serializable {

    /**
     * 题目 id
     */
    private Long id;

    /**
     * 题号（自定义题目 id）
     */
    private String customId;

    /**
     * 标题
     */
    private String title;

    private static final long serialVersionUID = 1L;
}
