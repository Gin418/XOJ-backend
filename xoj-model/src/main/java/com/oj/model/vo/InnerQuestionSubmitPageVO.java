package com.oj.model.vo;

import com.oj.model.entity.QuestionSubmit;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 题目服务内部返回的分页结果（竞赛提交记录）
 */
@Data
public class InnerQuestionSubmitPageVO implements Serializable {

    /**
     * 总条数
     */
    private long total;

    /**
     * 当前页记录
     */
    private List<QuestionSubmit> records;

    private static final long serialVersionUID = 1L;
}
