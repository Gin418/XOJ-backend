package com.oj.questionservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.model.entity.Discussion;

import java.util.Date;
import java.util.List;

/**
 * 讨论数据库操作
 */
public interface DiscussionMapper extends BaseMapper<Discussion> {

    /**
     * 查询讨论列表（包括已被删除的数据）
     */
    List<Discussion> listDiscussionWithDelete(Date minUpdateTime);

}




