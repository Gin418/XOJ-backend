package com.yupi.yuojbackendquestionservice.controller;

import com.yupi.yuojbackendcommon.common.BaseResponse;
import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.common.ResultUtils;
import com.yupi.yuojbackendcommon.exception.BusinessException;
import com.yupi.yuojbackendcommon.exception.ThrowUtils;
import com.yupi.yuojbackendcommon.utils.GatewayHeaderUtil;
import com.yupi.yuojbackendmodel.model.dto.discussionthumb.DiscussionThumbAddRequest;
import com.yupi.yuojbackendquestionservice.service.DiscussionThumbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/discussion_thumb")
@Slf4j
public class DiscussionThumbController {

    @Resource
    private DiscussionThumbService discussionThumbService;

    @PostMapping("/")
    public BaseResponse<Integer> doThumb(@RequestBody DiscussionThumbAddRequest discussionThumbAddRequest,
            HttpServletRequest request) {
        if (discussionThumbAddRequest == null || discussionThumbAddRequest.getDiscussionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long loginUserId = GatewayHeaderUtil.getUserId();
        ThrowUtils.throwIf(loginUserId <= 0, ErrorCode.NOT_LOGIN_ERROR);
        long discussionId = discussionThumbAddRequest.getDiscussionId();
        int result = discussionThumbService.doDiscussionThumb(discussionId, loginUserId);
        return ResultUtils.success(result);
    }

}
