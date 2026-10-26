package com.oj.model.dto.discussionthumb;

import lombok.Data;

import java.io.Serializable;

/**
 * 讨论点赞请求
 */
@Data
public class DiscussionThumbAddRequest implements Serializable {

    /**
     * 讨论 id
     */
    private Long discussionId;

    private static final long serialVersionUID = 1L;
}
