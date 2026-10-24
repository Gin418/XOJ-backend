package com.yupi.yuojbackendmodel.model.dto.discussion;

import com.yupi.yuojbackendcommon.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 查询请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DiscussionQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * id
     */
    private Long notId;

    /**
     * 搜索词
     */
    private String searchText;

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
     * 创建用户 id
     */
    private Long userId;

    /**
     * 父评论 id（查询某条评论的回复时传）
     */
    private Long parentId;

    /**
     * 是否只查一级评论（parentId 为 null 的记录）
     */
    private Boolean root;

    /**
     * 收藏用户 id
     */
    private Long favourUserId;

    private static final long serialVersionUID = 1L;
}
