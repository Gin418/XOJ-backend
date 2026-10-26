package com.oj.model.dto.discussionfavour;

import com.oj.common.common.PageRequest;
import com.oj.model.dto.discussion.DiscussionQueryRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 讨论收藏查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DiscussionFavourQueryRequest extends PageRequest implements Serializable {

    /**
     * 讨论查询请求
     */
    private DiscussionQueryRequest discussionQueryRequest;

    /**
     * 用户 id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}
