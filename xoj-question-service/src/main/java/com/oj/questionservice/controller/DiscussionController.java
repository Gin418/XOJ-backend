package com.oj.questionservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.common.annotation.AuthCheck;
import com.oj.common.common.BaseResponse;
import com.oj.common.common.DeleteRequest;
import com.oj.common.common.ErrorCode;
import com.oj.common.common.ResultUtils;
import com.oj.common.constant.UserConstant;
import com.oj.common.exception.BusinessException;
import com.oj.common.exception.ThrowUtils;
import com.oj.common.utils.GatewayHeaderUtil;
import com.oj.model.dto.discussion.DiscussionAddRequest;
import com.oj.model.dto.discussion.DiscussionEditRequest;
import com.oj.model.dto.discussion.DiscussionQueryRequest;
import com.oj.model.dto.discussion.DiscussionUpdateRequest;
import com.oj.model.entity.Discussion;
import com.oj.model.vo.DiscussionVO;
import com.oj.questionservice.service.DiscussionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/discussion")
@Slf4j
public class DiscussionController {

    @Resource
    private DiscussionService discussionService;


    @PostMapping("/add")
    public BaseResponse<Long> addDiscussion(@RequestBody DiscussionAddRequest discussionAddRequest, HttpServletRequest request) {
        if (discussionAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Discussion discussion = new Discussion();
        BeanUtils.copyProperties(discussionAddRequest, discussion);
        // 回复时校验父评论存在，题目 id 直接继承父评论，楼中楼不能再被回复（两级结构）
        Long parentId = discussionAddRequest.getParentId();
        if (parentId != null) {
            Discussion parent = discussionService.getById(parentId);
            ThrowUtils.throwIf(parent == null, ErrorCode.NOT_FOUND_ERROR, "回复的评论不存在");
            if (parent.getParentId() != null) {
                // 父评论本身是回复，仅当它是一级回复（其父级为主贴）时允许再回复
                Discussion grandParent = discussionService.getById(parent.getParentId());
                ThrowUtils.throwIf(grandParent == null || grandParent.getParentId() != null,
                        ErrorCode.PARAMS_ERROR, "楼中楼回复不能再被回复");
            }
            discussion.setQuestionId(parent.getQuestionId());
        }
        discussion.setReplyUserId(discussionAddRequest.getReplyUserId());
        discussionService.validDiscussion(discussion, true);
        discussion.setUserId(GatewayHeaderUtil.getUserId());
        discussion.setFavourNum(0);
        discussion.setThumbNum(0);
        boolean result = discussionService.save(discussion);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newDiscussionId = discussion.getId();
        return ResultUtils.success(newDiscussionId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteDiscussion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        Discussion oldDiscussion = discussionService.getById(id);
        ThrowUtils.throwIf(oldDiscussion == null, ErrorCode.NOT_FOUND_ERROR);
        if (!oldDiscussion.getUserId().equals(GatewayHeaderUtil.getUserId())
                && !UserConstant.ADMIN_ROLE.equals(GatewayHeaderUtil.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 连带删除该评论的全部两级回复（主贴 → 楼 → 楼中楼，共两级）
        List<Long> removeIds = new ArrayList<>();
        removeIds.add(id);
        List<Long> childIds = discussionService.lambdaQuery()
                .eq(Discussion::getParentId, id)
                .list().stream().map(Discussion::getId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(childIds)) {
            removeIds.addAll(childIds);
            removeIds.addAll(discussionService.lambdaQuery()
                    .in(Discussion::getParentId, childIds)
                    .list().stream().map(Discussion::getId).collect(Collectors.toList()));
        }
        boolean b = discussionService.removeByIds(removeIds);
        return ResultUtils.success(b);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateDiscussion(@RequestBody DiscussionUpdateRequest discussionUpdateRequest) {
        if (discussionUpdateRequest == null || discussionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Discussion discussion = new Discussion();
        BeanUtils.copyProperties(discussionUpdateRequest, discussion);
        discussionService.validDiscussion(discussion, false);
        long id = discussionUpdateRequest.getId();
        Discussion oldDiscussion = discussionService.getById(id);
        ThrowUtils.throwIf(oldDiscussion == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = discussionService.updateById(discussion);
        return ResultUtils.success(result);
    }

    @GetMapping("/get/vo")
    public BaseResponse<DiscussionVO> getDiscussionVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Discussion discussion = discussionService.getById(id);
        if (discussion == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(discussionService.getDiscussionVO(discussion, request));
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<DiscussionVO>> listDiscussionVOByPage(@RequestBody DiscussionQueryRequest discussionQueryRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(discussionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = discussionQueryRequest.getCurrent();
        long size = discussionQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 指定父评论时查询其回复列表（原有行为）
        if (discussionQueryRequest.getParentId() != null) {
            Page<Discussion> discussionPage = discussionService.page(new Page<>(current, size),
                    discussionService.getQueryWrapper(discussionQueryRequest));
            return ResultUtils.success(discussionService.getDiscussionVOPage(discussionPage, request));
        }
        // 主贴列表两级结构：第一级主贴（parentId 为 null），第二级每个主贴最早的 2 条楼
        discussionQueryRequest.setRoot(true);
        Page<Discussion> discussionPage = discussionService.page(new Page<>(current, size),
                discussionService.getQueryWrapper(discussionQueryRequest));
        Page<DiscussionVO> discussionVOPage = discussionService.getDiscussionVOPage(discussionPage, request);
        if (CollectionUtils.isNotEmpty(discussionVOPage.getRecords())) {
            List<Long> discussionIdList = discussionVOPage.getRecords().stream()
                    .map(DiscussionVO::getId).collect(Collectors.toList());
            Map<Long, List<DiscussionVO>> repliesMap = discussionService.listEarliestReplies(discussionIdList, request);
            discussionVOPage.getRecords().forEach(vo ->
                    vo.setReplies(repliesMap.getOrDefault(vo.getId(), Collections.emptyList())));
        }
        return ResultUtils.success(discussionVOPage);
    }

    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<DiscussionVO>> listMyDiscussionVOByPage(@RequestBody DiscussionQueryRequest discussionQueryRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(discussionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        discussionQueryRequest.setUserId(GatewayHeaderUtil.getUserId());
        // 我的讨论列表只返回主贴，回复记录通过 parentId 单独查询
        discussionQueryRequest.setRoot(true);
        long current = discussionQueryRequest.getCurrent();
        long size = discussionQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Discussion> discussionPage = discussionService.page(new Page<>(current, size),
                discussionService.getQueryWrapper(discussionQueryRequest));
        return ResultUtils.success(discussionService.getDiscussionVOPage(discussionPage, request));
    }

    // endregion

    @PostMapping("/search/page/vo")
    public BaseResponse<Page<DiscussionVO>> searchDiscussionVOByPage(@RequestBody DiscussionQueryRequest discussionQueryRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(discussionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 搜索仅针对主贴
        discussionQueryRequest.setRoot(true);
        long current = discussionQueryRequest.getCurrent();
        long size = discussionQueryRequest.getPageSize();

        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Discussion> discussionPage = discussionService.page(new Page<>(current, size),
                discussionService.getQueryWrapper(discussionQueryRequest));
        return ResultUtils.success(discussionService.getDiscussionVOPage(discussionPage, request));
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editDiscussion(@RequestBody DiscussionEditRequest discussionEditRequest, HttpServletRequest request) {
        if (discussionEditRequest == null || discussionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Discussion discussion = new Discussion();
        BeanUtils.copyProperties(discussionEditRequest, discussion);
        discussionService.validDiscussion(discussion, false);
        long id = discussionEditRequest.getId();

        Discussion oldDiscussion = discussionService.getById(id);
        ThrowUtils.throwIf(oldDiscussion == null, ErrorCode.NOT_FOUND_ERROR);

        if (!oldDiscussion.getUserId().equals(GatewayHeaderUtil.getUserId())
                && !UserConstant.ADMIN_ROLE.equals(GatewayHeaderUtil.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = discussionService.updateById(discussion);
        return ResultUtils.success(result);
    }

}
