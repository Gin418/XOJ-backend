package com.oj.questionservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oj.common.common.ErrorCode;
import com.oj.common.constant.CommonConstant;
import com.oj.common.exception.BusinessException;
import com.oj.common.exception.ThrowUtils;
import com.oj.common.utils.SqlUtils;
import com.oj.model.dto.discussion.DiscussionQueryRequest;
import com.oj.model.entity.Discussion;
import com.oj.model.entity.DiscussionFavour;
import com.oj.model.entity.DiscussionThumb;
import com.oj.model.entity.User;
import com.oj.model.vo.DiscussionVO;
import com.oj.model.vo.UserVO;
import com.oj.questionservice.mapper.DiscussionFavourMapper;
import com.oj.questionservice.mapper.DiscussionMapper;
import com.oj.questionservice.mapper.DiscussionThumbMapper;
import com.oj.questionservice.service.DiscussionService;
import com.oj.serviceclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 讨论服务实现
 */
@Service
@Slf4j
public class DiscussionServiceImpl extends ServiceImpl<DiscussionMapper, Discussion> implements DiscussionService {

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private DiscussionThumbMapper discussionThumbMapper;

    @Resource
    private DiscussionFavourMapper discussionFavourMapper;

    @Override
    public void validDiscussion(Discussion discussion, boolean add) {
        if (discussion == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = discussion.getTitle();
        String content = discussion.getContent();
        Long questionId = discussion.getQuestionId();
        // 创建时，参数不能为空
        if (add) {
            // 一级评论标题与正文均必填；回复（有父评论）时标题可空，正文必填
            if (discussion.getParentId() == null) {
                ThrowUtils.throwIf(StringUtils.isAnyBlank(title, content), ErrorCode.PARAMS_ERROR);
            } else {
                ThrowUtils.throwIf(StringUtils.isBlank(content), ErrorCode.PARAMS_ERROR);
            }
            ThrowUtils.throwIf(questionId == null || questionId <= 0, ErrorCode.PARAMS_ERROR, "题目 id 不能为空");
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(title) && title.length() > 80) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
        if (StringUtils.isNotBlank(content) && content.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容过长");
        }
    }

    /**
     * 获取查询包装类
     *
     * @param discussionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Discussion> getQueryWrapper(DiscussionQueryRequest discussionQueryRequest) {
        QueryWrapper<Discussion> queryWrapper = new QueryWrapper<>();
        if (discussionQueryRequest == null) {
            return queryWrapper;
        }
        String searchText = discussionQueryRequest.getSearchText();
        String sortField = discussionQueryRequest.getSortField();
        String sortOrder = discussionQueryRequest.getSortOrder();
        Long id = discussionQueryRequest.getId();
        String title = discussionQueryRequest.getTitle();
        String content = discussionQueryRequest.getContent();
        Long questionId = discussionQueryRequest.getQuestionId();
        Long parentId = discussionQueryRequest.getParentId();
        Boolean root = discussionQueryRequest.getRoot();
        Long userId = discussionQueryRequest.getUserId();
        Long notId = discussionQueryRequest.getNotId();
        // 拼接查询条件
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(parentId), "parentId", parentId);
        queryWrapper.isNull(Boolean.TRUE.equals(root), "parentId");
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), CommonConstant.SORT_ORDER_ASC.equals(sortOrder),
                sortField);
        return queryWrapper;
    }

    @Override
    public DiscussionVO getDiscussionVO(Discussion discussion, HttpServletRequest request) {
        DiscussionVO discussionVO = DiscussionVO.objToVo(discussion);
        long discussionId = discussion.getId();
        // 1. 关联查询用户信息
        Long userId = discussion.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userFeignClient.getById(userId);
        }
        UserVO userVO = userFeignClient.getUserVO(user);
        discussionVO.setUser(userVO);
        // 1.1 回复补充：被回复用户信息（优先冗余字段，存量数据回退查父评论）
        Long parentId = discussion.getParentId();
        Long replyUserId = discussion.getReplyUserId();
        if (replyUserId == null && parentId != null) {
            Discussion parent = getById(parentId);
            if (parent != null) {
                replyUserId = parent.getUserId();
                discussionVO.setReplyUserId(replyUserId);
            }
        }
        if (replyUserId != null) {
            User replyUser = userFeignClient.getById(replyUserId);
            discussionVO.setReplyUser(userFeignClient.getUserVO(replyUser));
        }
        // 1.2 回复数（一级评论展示“查看回复”用）
        discussionVO.setReplyCount(lambdaQuery()
                .eq(Discussion::getParentId, discussionId)
                .count().intValue());
        // 2. 已登录，获取用户点赞、收藏状态
        User loginUser = userFeignClient.getLoginUserPermitNull(request);
        if (loginUser != null) {
            // 获取点赞
            QueryWrapper<DiscussionThumb> discussionThumbQueryWrapper = new QueryWrapper<>();
            discussionThumbQueryWrapper.in("discussionId", discussionId);
            discussionThumbQueryWrapper.eq("userId", loginUser.getId());
            DiscussionThumb discussionThumb = discussionThumbMapper.selectOne(discussionThumbQueryWrapper);
            discussionVO.setHasThumb(discussionThumb != null);
            // 获取收藏
            QueryWrapper<DiscussionFavour> discussionFavourQueryWrapper = new QueryWrapper<>();
            discussionFavourQueryWrapper.in("discussionId", discussionId);
            discussionFavourQueryWrapper.eq("userId", loginUser.getId());
            DiscussionFavour discussionFavour = discussionFavourMapper.selectOne(discussionFavourQueryWrapper);
            discussionVO.setHasFavour(discussionFavour != null);
        }
        return discussionVO;
    }

    @Override
    public Page<DiscussionVO> getDiscussionVOPage(Page<Discussion> discussionPage, HttpServletRequest request) {
        List<Discussion> discussionList = discussionPage.getRecords();
        Page<DiscussionVO> discussionVOPage = new Page<>(discussionPage.getCurrent(), discussionPage.getSize(), discussionPage.getTotal());
        if (CollectionUtils.isEmpty(discussionList)) {
            return discussionVOPage;
        }
        // 1. 关联查询用户信息（含被回复用户：优先冗余字段 replyUserId，存量无值记录回退查父评论）
        Set<Long> userIdSet = discussionList.stream().map(Discussion::getUserId).collect(Collectors.toSet());
        discussionList.stream().map(Discussion::getReplyUserId).filter(ObjectUtils::isNotEmpty)
                .forEach(userIdSet::add);
        Set<Long> parentIdSet = discussionList.stream().filter(d -> d.getReplyUserId() == null)
                .map(Discussion::getParentId)
                .filter(ObjectUtils::isNotEmpty).collect(Collectors.toSet());
        Map<Long, Discussion> parentMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(parentIdSet)) {
            List<Discussion> parentList = listByIds(parentIdSet);
            parentList.forEach(parent -> {
                parentMap.put(parent.getId(), parent);
                if (parent.getUserId() != null) {
                    userIdSet.add(parent.getUserId());
                }
            });
        }
        Map<Long, List<User>> userIdUserListMap = userFeignClient.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 1.1 批量统计每条评论的回复数（仅一级评论有意义）
        Map<Long, Integer> replyCountMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(discussionList)) {
            Set<Long> discussionIdSet = discussionList.stream().map(Discussion::getId).collect(Collectors.toSet());
            QueryWrapper<Discussion> countWrapper = new QueryWrapper<>();
            countWrapper.select("parentId", "count(*) as cnt");
            countWrapper.in("parentId", discussionIdSet);
            countWrapper.eq("isDelete", false);
            countWrapper.groupBy("parentId");
            listMaps(countWrapper).forEach(row -> {
                Object pid = row.get("parentId");
                Object cnt = row.get("cnt");
                if (pid != null && cnt != null) {
                    replyCountMap.put(Long.valueOf(pid.toString()), Integer.valueOf(cnt.toString()));
                }
            });
        }
        // 2. 已登录，获取用户点赞、收藏状态
        Map<Long, Boolean> discussionIdHasThumbMap = new HashMap<>();
        Map<Long, Boolean> discussionIdHasFavourMap = new HashMap<>();
        User loginUser = userFeignClient.getLoginUserPermitNull(request);
        if (loginUser != null) {
            Set<Long> discussionIdSet = discussionList.stream().map(Discussion::getId).collect(Collectors.toSet());
            // 获取点赞
            QueryWrapper<DiscussionThumb> discussionThumbQueryWrapper = new QueryWrapper<>();
            discussionThumbQueryWrapper.in("discussionId", discussionIdSet);
            discussionThumbQueryWrapper.eq("userId", loginUser.getId());
            List<DiscussionThumb> discussionDiscussionThumbList = discussionThumbMapper.selectList(discussionThumbQueryWrapper);
            discussionDiscussionThumbList.forEach(discussionDiscussionThumb -> discussionIdHasThumbMap.put(discussionDiscussionThumb.getDiscussionId(), true));
            // 获取收藏
            QueryWrapper<DiscussionFavour> discussionFavourQueryWrapper = new QueryWrapper<>();
            discussionFavourQueryWrapper.in("discussionId", discussionIdSet);
            discussionFavourQueryWrapper.eq("userId", loginUser.getId());
            List<DiscussionFavour> discussionFavourList = discussionFavourMapper.selectList(discussionFavourQueryWrapper);
            discussionFavourList.forEach(discussionFavour -> discussionIdHasFavourMap.put(discussionFavour.getDiscussionId(), true));
        }
        // 填充信息
        List<DiscussionVO> discussionVOList = discussionList.stream().map(discussion -> {
            DiscussionVO discussionVO = DiscussionVO.objToVo(discussion);
            Long userId = discussion.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            discussionVO.setUser(userFeignClient.getUserVO(user));
            // 被回复用户信息（回复展示“回复 @xxx”，优先冗余字段，存量回退父评论）
            Long replyUserId = discussion.getReplyUserId();
            if (replyUserId == null && discussion.getParentId() != null) {
                Discussion parent = parentMap.get(discussion.getParentId());
                if (parent != null) {
                    replyUserId = parent.getUserId();
                    discussionVO.setReplyUserId(replyUserId);
                }
            }
            if (replyUserId != null && userIdUserListMap.containsKey(replyUserId)) {
                discussionVO.setReplyUser(userFeignClient.getUserVO(userIdUserListMap.get(replyUserId).get(0)));
            }
            // 回复数
            discussionVO.setReplyCount(replyCountMap.getOrDefault(discussion.getId(), 0));
            discussionVO.setHasThumb(discussionIdHasThumbMap.getOrDefault(discussion.getId(), false));
            discussionVO.setHasFavour(discussionIdHasFavourMap.getOrDefault(discussion.getId(), false));
            return discussionVO;
        }).collect(Collectors.toList());
        discussionVOPage.setRecords(discussionVOList);
        return discussionVOPage;
    }

    @Override
    public Map<Long, List<DiscussionVO>> listEarliestReplies(List<Long> discussionIdList, HttpServletRequest request) {
        Map<Long, List<DiscussionVO>> result = new HashMap<>();
        if (CollectionUtils.isEmpty(discussionIdList)) {
            return result;
        }
        // 查询这些主贴的全部楼（按创建时间升序），内存分组每主贴取最早 2 条
        List<Discussion> replyList = lambdaQuery()
                .in(Discussion::getParentId, discussionIdList)
                .orderByAsc(Discussion::getCreateTime)
                .list();
        if (CollectionUtils.isEmpty(replyList)) {
            return result;
        }
        Map<Long, Integer> replyNumMap = new HashMap<>();
        List<Discussion> earliestReplyList = new ArrayList<>();
        for (Discussion reply : replyList) {
            int num = replyNumMap.merge(reply.getParentId(), 1, Integer::sum);
            if (num <= 2) {
                earliestReplyList.add(reply);
            }
        }
        // 复用分页封装填充用户信息、被回复用户、点赞收藏状态等
        Page<Discussion> replyPage = new Page<>(1, earliestReplyList.size(), earliestReplyList.size());
        replyPage.setRecords(earliestReplyList);
        Page<DiscussionVO> replyVOPage = getDiscussionVOPage(replyPage, request);
        return replyVOPage.getRecords().stream()
                .collect(Collectors.groupingBy(DiscussionVO::getParentId));
    }

}
