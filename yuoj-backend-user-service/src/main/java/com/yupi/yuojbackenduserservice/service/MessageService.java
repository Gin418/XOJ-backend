package com.yupi.yuojbackenduserservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yuojbackendmodel.model.entity.Message;
import com.yupi.yuojbackendmodel.model.entity.User;
import com.yupi.yuojbackendmodel.model.vo.ConversationVO;
import com.yupi.yuojbackendmodel.model.vo.MessageUnreadVO;
import com.yupi.yuojbackendmodel.model.vo.MessageVO;

/**
 * 消息服务（系统通知 / 私信）
 *
 * @author Q
 */
public interface MessageService extends IService<Message> {

    /**
     * 发送私信：落库后经 MQ 广播，由接收方所在实例推送
     *
     * @param sender     发送用户（当前登录用户）
     * @param receiverId 接收用户 id
     * @param content    消息内容
     * @return 发送成功的消息（含发送者信息）
     */
    MessageVO sendPrivateMessage(User sender, Long receiverId, String content);

    /**
     * 发送系统通知
     *
     * @param title      标题
     * @param content    内容
     * @param receiverId 接收用户 id（为空则仅广播给所有在线用户，不落库）
     */
    void sendNotification(String title, String content, Long receiverId);

    /**
     * 分页查询消息列表
     *
     * @param current  当前页号
     * @param pageSize 页面大小
     * @param type     消息类型：0 系统通知、1 私信
     * @param friendId 好友用户 id（私信会话查询使用，可为空）
     * @param userId   当前登录用户 id
     * @return
     */
    Page<MessageVO> listMessages(long current, long pageSize, Integer type, Long friendId, Long userId);

    /**
     * 标记单条消息已读
     *
     * @param messageId 消息 id
     * @param userId    当前登录用户 id
     * @return
     */
    boolean markRead(Long messageId, Long userId);

    /**
     * 标记全部已读
     *
     * @param type     消息类型：0 系统通知、1 私信
     * @param friendId 好友用户 id（私信会话全部已读使用）
     * @param userId   当前登录用户 id
     * @return
     */
    boolean markAllRead(Integer type, Long friendId, Long userId);

    /**
     * 未读消息数量（按类型拆分：系统通知、私信）
     *
     * @param userId 当前登录用户 id
     * @return 系统通知与私信的未读数量
     */
    MessageUnreadVO unreadCount(Long userId);

    /**
     * 分页查询私信会话列表（每个好友一个会话，按最后消息时间倒序）
     *
     * @param current  当前页号
     * @param pageSize 页面大小
     * @param userId   当前登录用户 id
     * @return 会话列表（含好友信息、最后一条消息、未读数）
     */
    Page<ConversationVO> listConversations(long current, long pageSize, Long userId);

    /**
     * 打开会话：将该好友发来的私信标记已读，并返回会话配置。
     * 若无历史消息，也返回含对方信息的空会话配置（不落库）。
     *
     * @param friendId 好友用户 id
     * @param userId   当前登录用户 id
     * @return 会话配置（对方信息、未读数、第一条未读消息 id、最后一条消息预览等）
     */
    ConversationVO openConversation(Long friendId, Long userId);
}
