package com.oj.model.vo;

import com.oj.model.codesandbox.JudgeInfo;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 竞赛提交记录视图
 */
@Data
public class CompetitionSubmitVO implements Serializable {

    /**
     * 提交 id
     */
    private Long id;

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 题号（A、B、C……）
     */
    private String customId;

    /**
     * 题目标题
     */
    private String questionTitle;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 测评状态
     */
    private String status;

    /**
     * 判题信息
     */
    private JudgeInfo judgeInfo;

    /**
     * 代码文件大小（字节）
     */
    private Long codeSize;

    /**
     * 提交用户 id
     */
    private Long userId;

    /**
     * 提交代码（仅自己提交的代码返回，他人提交的为 null）
     */
    private String code;

    /**
     * 提交用户信息（脱敏）
     */
    private UserVO userVO;

    /**
     * 提交时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
