package com.oj.questionservice.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.model.entity.Discussion;
import com.oj.model.entity.DiscussionFavour;
import org.apache.ibatis.annotations.Param;

/**
 * 讨论收藏数据库操作
 */
public interface DiscussionFavourMapper extends BaseMapper<DiscussionFavour> {

    /**
     * 分页查询收藏讨论列表
     *
     * @param page
     * @param queryWrapper
     * @param favourUserId
     * @return
     */
    Page<Discussion> listFavourDiscussionByPage(IPage<Discussion> page, @Param(Constants.WRAPPER) Wrapper<Discussion> queryWrapper,
            long favourUserId);

}




