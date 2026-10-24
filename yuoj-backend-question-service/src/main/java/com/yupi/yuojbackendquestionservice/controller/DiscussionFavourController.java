package com.yupi.yuojbackendquestionservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yuojbackendcommon.common.BaseResponse;
import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.common.ResultUtils;
import com.yupi.yuojbackendcommon.exception.BusinessException;
import com.yupi.yuojbackendcommon.exception.ThrowUtils;
import com.yupi.yuojbackendcommon.utils.GatewayHeaderUtil;
import com.yupi.yuojbackendmodel.model.dto.discussion.DiscussionQueryRequest;
import com.yupi.yuojbackendmodel.model.dto.discussionfavour.DiscussionFavourAddRequest;
import com.yupi.yuojbackendmodel.model.dto.discussionfavour.DiscussionFavourQueryRequest;
import com.yupi.yuojbackendmodel.model.entity.Discussion;
import com.yupi.yuojbackendmodel.model.vo.DiscussionVO;
import com.yupi.yuojbackendquestionservice.service.DiscussionFavourService;
import com.yupi.yuojbackendquestionservice.service.DiscussionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/discussion_favour")
@Slf4j
public class DiscussionFavourController {

    @Resource
    private DiscussionFavourService discussionFavourService;

    @Resource
    private DiscussionService discussionService;

    @PostMapping("/")
    public BaseResponse<Integer> doDiscussionFavour(@RequestBody DiscussionFavourAddRequest discussionFavourAddRequest,
            HttpServletRequest request) {
        if (discussionFavourAddRequest == null || discussionFavourAddRequest.getDiscussionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long loginUserId = GatewayHeaderUtil.getUserId();
        ThrowUtils.throwIf(loginUserId <= 0, ErrorCode.NOT_LOGIN_ERROR);
        long discussionId = discussionFavourAddRequest.getDiscussionId();
        int result = discussionFavourService.doDiscussionFavour(discussionId, loginUserId);
        return ResultUtils.success(result);
    }

    @PostMapping("/my/list/page")
    public BaseResponse<Page<DiscussionVO>> listMyFavourDiscussionByPage(@RequestBody DiscussionQueryRequest discussionQueryRequest,
            HttpServletRequest request) {
        if (discussionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long loginUserId = GatewayHeaderUtil.getUserId();
        ThrowUtils.throwIf(loginUserId <= 0, ErrorCode.NOT_LOGIN_ERROR);
        long current = discussionQueryRequest.getCurrent();
        long size = discussionQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Discussion> discussionPage = discussionFavourService.listFavourDiscussionByPage(new Page<>(current, size),
                discussionService.getQueryWrapper(discussionQueryRequest), loginUserId);
        return ResultUtils.success(discussionService.getDiscussionVOPage(discussionPage, request));
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<DiscussionVO>> listFavourDiscussionByPage(@RequestBody DiscussionFavourQueryRequest discussionFavourQueryRequest,
            HttpServletRequest request) {
        if (discussionFavourQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = discussionFavourQueryRequest.getCurrent();
        long size = discussionFavourQueryRequest.getPageSize();
        Long userId = discussionFavourQueryRequest.getUserId();
        ThrowUtils.throwIf(size > 20 || userId == null, ErrorCode.PARAMS_ERROR);
        Page<Discussion> discussionPage = discussionFavourService.listFavourDiscussionByPage(new Page<>(current, size),
                discussionService.getQueryWrapper(discussionFavourQueryRequest.getDiscussionQueryRequest()), userId);
        return ResultUtils.success(discussionService.getDiscussionVOPage(discussionPage, request));
    }
}
