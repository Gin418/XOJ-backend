package com.oj.questionservice.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.oj.model.entity.Discussion;
import com.oj.model.entity.DiscussionFavour;

/**
 * 讨论收藏服务
 */
public interface DiscussionFavourService extends IService<DiscussionFavour> {

    /**
     * 讨论收藏
     *
     * @param discussionId
     * @param userId
     * @return
     */
    int doDiscussionFavour(long discussionId, long userId);

    /**
     * 分页获取用户收藏的讨论列表
     *
     * @param page
     * @param queryWrapper
     * @param favourUserId
     * @return
     */
    Page<Discussion> listFavourDiscussionByPage(IPage<Discussion> page, Wrapper<Discussion> queryWrapper,
            long favourUserId);

    /**
     * 讨论收藏（内部服务）
     *
     * @param userId
     * @param discussionId
     * @return
     */
    int doDiscussionFavourInner(long userId, long discussionId);
}
