package com.oj.model.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建请求
 */
@Data
public class QuestionAddRequest implements Serializable {

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 自定义题号
     */
    private String customId;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 题目说明
     */
    private String description;

    /**
     * 测试点路径
     **/
    private String inOutPath;

    /**
     * 判题用例
     */
    private List<JudgeCase> judgeCase;

    /**
     * 判题配置
     */
    private JudgeConfig judgeConfig;

    private static final long serialVersionUID = 1L;
}
