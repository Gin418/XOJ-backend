package com.oj.questionservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oj.model.entity.DiscussionThumb;

/**
 * 讨论点赞服务
 */
public interface DiscussionThumbService extends IService<DiscussionThumb> {

    /**
     * 点赞
     *
     * @param discussionId
     * @param userId
     * @return
     */
    int doDiscussionThumb(long discussionId, long userId);

    /**
     * 讨论点赞（内部服务）
     *
     * @param userId
     * @param discussionId
     * @return
     */
    int doDiscussionThumbInner(long userId, long discussionId);
}
