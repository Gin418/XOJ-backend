package com.yupi.yuojbackendquestionservice.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yuojbackendmodel.model.entity.Discussion;
import com.yupi.yuojbackendmodel.model.entity.DiscussionFavour;
import org.apache.ibatis.annotations.Param;

/**
 * 讨论收藏数据库操作
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
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




