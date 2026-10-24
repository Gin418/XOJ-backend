package com.yupi.yuojbackendmodel.model.dto.discussion;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class DiscussionAddRequest implements Serializable {

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 回复用户 id
     */
    private Long replyUserId;

    /**
     * 父评论 id（回复时非空，null 表示一级评论）
     */
    private Long parentId;

    private static final long serialVersionUID = 1L;
}
