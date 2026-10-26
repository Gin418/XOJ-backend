package com.oj.questionservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.oj.model.dto.discussion.DiscussionQueryRequest;
import com.oj.model.entity.Discussion;
import com.oj.model.vo.DiscussionVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 讨论服务
 */
public interface DiscussionService extends IService<Discussion> {

    /**
     * 校验
     *
     * @param discussion
     * @param add
     */
    void validDiscussion(Discussion discussion, boolean add);

    /**
     * 获取查询条件
     *
     * @param discussionQueryRequest
     * @return
     */
    QueryWrapper<Discussion> getQueryWrapper(DiscussionQueryRequest discussionQueryRequest);

    /**
     * 获取讨论封装
     *
     * @param discussion
     * @param request
     * @return
     */
    DiscussionVO getDiscussionVO(Discussion discussion, HttpServletRequest request);

    /**
     * 分页获取讨论封装
     *
     * @param discussionPage
     * @param request
     * @return
     */
    Page<DiscussionVO> getDiscussionVOPage(Page<Discussion> discussionPage, HttpServletRequest request);

    /**
     * 批量获取主贴下最早的回复（每个主贴最多 2 条，按创建时间升序）
     *
     * @param discussionIdList 主贴 id 列表
     * @param request
     * @return 主贴 id -> 最早回复列表
     */
    Map<Long, List<DiscussionVO>> listEarliestReplies(List<Long> discussionIdList, HttpServletRequest request);
}
