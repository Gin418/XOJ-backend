package com.oj.model.dto.questionsubmit;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建请求
 */
@Data
public class QuestionSubmitAddRequest implements Serializable {

    /**
     * 编程语言id
     */
    private Integer languageId;

    /**
     * 用户代码
     */
    private String code;

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 竞赛 id（null 表示题库提交）
     */
    private Long competitionId;

    private static final long serialVersionUID = 1L;
}
