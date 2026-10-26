package com.yupi.yuojbackenduserservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yuojbackendcommon.annotation.AuthCheck;
import com.yupi.yuojbackendcommon.common.BaseResponse;
import com.yupi.yuojbackendcommon.common.DeleteRequest;
import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.common.PageRequest;
import com.yupi.yuojbackendcommon.common.ResultUtils;
import com.yupi.yuojbackendcommon.constant.UserConstant;
import com.yupi.yuojbackendcommon.exception.BusinessException;
import com.yupi.yuojbackendcommon.exception.ThrowUtils;
import com.yupi.yuojbackendmodel.model.dto.message.ConversationOpenRequest;
import com.yupi.yuojbackendmodel.model.dto.message.MessageQueryRequest;
import com.yupi.yuojbackendmodel.model.dto.message.MessageReadAllRequest;
import com.yupi.yuojbackendmodel.model.dto.message.MessageSendRequest;
import com.yupi.yuojbackendmodel.model.dto.message.NotificationSendRequest;
import com.yupi.yuojbackendmodel.model.entity.User;
import com.yupi.yuojbackendmodel.model.vo.ConversationVO;
import com.yupi.yuojbackendmodel.model.vo.MessageUnreadVO;
import com.yupi.yuojbackendmodel.model.vo.MessageVO;
import com.yupi.yuojbackenduserservice.service.MessageService;
import com.yupi.yuojbackenduserservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 消息接口（系统通知 / 私信）
 *
 * @author Q
 */
@RestController
@RequestMapping("/message")
@Slf4j
public class MessageController {

    @Resource
    private MessageService messageService;

    @Resource
    private UserService userService;

    /**
     * 发送私信
     *
     * @param messageSendRequest
     * @param request
     * @return
     */
    @PostMapping("/send")
    public BaseResponse<MessageVO> sendPrivateMessage(@RequestBody MessageSendRequest messageSendRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(messageSendRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        MessageVO messageVO = messageService.sendPrivateMessage(loginUser,
                messageSendRequest.getReceiverId(), messageSendRequest.getContent());
        return ResultUtils.success(messageVO);
    }

    /**
     * 发送系统通知（仅管理员，receiverId 为空则广播给所有在线用户）
     *
     * @param notificationSendRequest
     * @return
     */
    @PostMapping("/notification/send")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> sendNotification(@RequestBody NotificationSendRequest notificationSendRequest) {
        ThrowUtils.throwIf(notificationSendRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StringUtils.isAnyBlank(notificationSendRequest.getTitle(), notificationSendRequest.getContent()),
                ErrorCode.PARAMS_ERROR, "标题和内容不能为空");
        messageService.sendNotification(notificationSendRequest.getTitle(),
                notificationSendRequest.getContent(), notificationSendRequest.getReceiverIds());
        return ResultUtils.success(true);
    }

    /**
     * 分页查询消息列表
     *
     * @param messageQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<MessageVO>> listMessages(@RequestBody MessageQueryRequest messageQueryRequest,
                                                      HttpServletRequest request) {
        if (messageQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Page<MessageVO> messageVOPage = messageService.listMessages(messageQueryRequest.getCurrent(),
                messageQueryRequest.getPageSize(), messageQueryRequest.getType(),
                messageQueryRequest.getFriendId(), loginUser.getId());
        return ResultUtils.success(messageVOPage);
    }

    /**
     * 标记单条消息已读
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/read")
    public BaseResponse<Boolean> markRead(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(messageService.markRead(deleteRequest.getId(), loginUser.getId()));
    }

    /**
     * 标记全部已读
     *
     * @param messageReadAllRequest
     * @param request
     * @return
     */
    @PostMapping("/read/all")
    public BaseResponse<Boolean> markAllRead(@RequestBody MessageReadAllRequest messageReadAllRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(messageReadAllRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(messageService.markAllRead(messageReadAllRequest.getType(),
                messageReadAllRequest.getFriendId(), loginUser.getId()));
    }

    /**
     * 未读消息数量（按类型拆分：系统通知、私信）
     *
     * @param request
     * @return
     */
    @GetMapping("/unread")
    public BaseResponse<MessageUnreadVO> unreadCount(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(messageService.unreadCount(loginUser.getId()));
    }

    /**
     * 查询私信会话列表（每个好友一个会话，按最后消息时间倒序）
     *
     * @param pageRequest
     * @param request
     * @return
     */
    @PostMapping("/conversation/list/page")
    public BaseResponse<Page<ConversationVO>> listConversations(@RequestBody PageRequest pageRequest,
                                                                HttpServletRequest request) {
        ThrowUtils.throwIf(pageRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(messageService.listConversations(pageRequest.getCurrent(),
                pageRequest.getPageSize(), loginUser.getId()));
    }

    /**
     * 打开会话：标记该好友发来的私信已读，并返回会话配置（无历史消息也返回含对方信息的空会话）
     *
     * @param conversationOpenRequest
     * @param request
     * @return
     */
    @PostMapping("/conversation/open")
    public BaseResponse<ConversationVO> openConversation(@RequestBody ConversationOpenRequest conversationOpenRequest,
                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(conversationOpenRequest == null || conversationOpenRequest.getFriendId() == null
                || conversationOpenRequest.getFriendId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(messageService.openConversation(conversationOpenRequest.getFriendId(),
                loginUser.getId()));
    }
}
