package com.oj.model.dto.discussionfavour;

import lombok.Data;

import java.io.Serializable;

/**
 * 讨论收藏 / 取消收藏请求
 */
@Data
public class DiscussionFavourAddRequest implements Serializable {

    /**
     * 讨论 id
     */
    private Long discussionId;

    private static final long serialVersionUID = 1L;
}
