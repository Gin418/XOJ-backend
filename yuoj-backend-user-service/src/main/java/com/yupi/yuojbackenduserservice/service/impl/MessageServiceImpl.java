package com.yupi.yuojbackenduserservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.exception.ThrowUtils;
import com.yupi.yuojbackendmodel.model.entity.Conversation;
import com.yupi.yuojbackendmodel.model.entity.Message;
import com.yupi.yuojbackendmodel.model.entity.User;
import com.yupi.yuojbackendmodel.model.enums.MessageTypeEnum;
import com.yupi.yuojbackendmodel.model.vo.ConversationVO;
import com.yupi.yuojbackendmodel.model.vo.MessageUnreadVO;
import com.yupi.yuojbackendmodel.model.vo.MessageVO;
import com.yupi.yuojbackenduserservice.mapper.ConversationMapper;
import com.yupi.yuojbackenduserservice.mapper.MessageMapper;
import com.yupi.yuojbackenduserservice.mapper.UserMapper;
import com.yupi.yuojbackenduserservice.rabbitmq.MessageMqProducer;
import com.yupi.yuojbackenduserservice.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 消息服务实现
 *
 * @author Q
 */
@Service
@Slf4j
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    /**
     * 私信内容最大长度
     */
    private static final int CONTENT_MAX_LENGTH = 5000;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ConversationMapper conversationMapper;

    @Resource
    private MessageMqProducer messageMqProducer;

    @Override
    public MessageVO sendPrivateMessage(User sender, Long receiverId, String content) {
        // 1. 参数校验
        ThrowUtils.throwIf(receiverId == null || receiverId <= 0, ErrorCode.PARAMS_ERROR, "接收用户不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(content), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(content.length() > CONTENT_MAX_LENGTH, ErrorCode.PARAMS_ERROR, "消息内容过长");
        // 2. 校验接收用户存在
        User receiver = userMapper.selectById(receiverId);
        ThrowUtils.throwIf(receiver == null, ErrorCode.NOT_FOUND_ERROR, "接收用户不存在");
        // 3. 落库
        Message message = new Message();
        message.setType(MessageTypeEnum.PRIVATE.getValue());
        message.setContent(content);
        message.setSenderId(sender.getId());
        message.setReceiverId(receiverId);
        message.setIsRead(Message.UNREAD);
        boolean saved = this.save(message);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "消息发送失败");
        // 4. 维护双方会话记录（更新最后一条消息）
        upsertConversation(sender.getId(), receiverId, message.getId());
        upsertConversation(receiverId, sender.getId(), message.getId());
        // 5. 构造 VO（含发送者信息），经 MQ 广播后由接收方所在实例推送
        MessageVO messageVO = new MessageVO();
        BeanUtils.copyProperties(message, messageVO);
        messageVO.setSenderName(sender.getUserName());
        messageVO.setSenderAvatar(sender.getUserAvatar());
        messageMqProducer.publish(Collections.singletonList(messageVO));
        return messageVO;
    }

    @Override
    public void sendNotification(String title, String content, List<Long> receiverIds) {
        ThrowUtils.throwIf(StringUtils.isAnyBlank(title, content), ErrorCode.PARAMS_ERROR, "标题和内容不能为空");
        // receiverIds 为空表示广播通知：只推送给在线用户，不落库
        if (CollectionUtils.isEmpty(receiverIds)) {
            MessageVO messageVO = new MessageVO();
            messageVO.setType(MessageTypeEnum.NOTIFICATION.getValue());
            messageVO.setTitle(title);
            messageVO.setContent(content);
            messageVO.setIsRead(Message.UNREAD);
            messageMqProducer.publish(Collections.singletonList(messageVO));
            return;
        }
        // 去重并过滤无效 id，批量校验接收用户存在
        List<Long> distinctReceiverIds = receiverIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());
        ThrowUtils.throwIf(distinctReceiverIds.isEmpty(), ErrorCode.PARAMS_ERROR, "接收用户不能为空");
        List<User> receivers = userMapper.selectBatchIds(distinctReceiverIds);
        ThrowUtils.throwIf(receivers.size() != distinctReceiverIds.size(), ErrorCode.NOT_FOUND_ERROR, "存在无效的接收用户");
        // 每个接收用户落库一条通知，最后只发一条 MQ 消息
        List<MessageVO> messageVOList = new ArrayList<>();
        for (Long receiverId : distinctReceiverIds) {
            Message message = new Message();
            message.setType(MessageTypeEnum.NOTIFICATION.getValue());
            message.setTitle(title);
            message.setContent(content);
            message.setReceiverId(receiverId);
            message.setIsRead(Message.UNREAD);
            boolean saved = this.save(message);
            ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "通知发送失败");
            MessageVO messageVO = new MessageVO();
            BeanUtils.copyProperties(message, messageVO);
            messageVOList.add(messageVO);
        }
        messageMqProducer.publish(messageVOList);
    }

    @Override
    public Page<MessageVO> listMessages(long current, long pageSize, Integer type, Long friendId, Long userId) {
        MessageTypeEnum typeEnum = MessageTypeEnum.getEnumByValue(type);
        ThrowUtils.throwIf(typeEnum == null, ErrorCode.PARAMS_ERROR, "消息类型错误");
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", type);
        if (MessageTypeEnum.PRIVATE.equals(typeEnum)) {
            // 私信：与好友的双向对话（friendId 为空则查询与我相关的全部私信）
            if (friendId != null) {
                queryWrapper.and(q -> q.eq("senderId", userId).eq("receiverId", friendId)
                        .or()
                        .eq("senderId", friendId).eq("receiverId", userId));
            } else {
                queryWrapper.and(q -> q.eq("senderId", userId).or().eq("receiverId", userId));
            }
        } else {
            // 系统通知：发给我的
            queryWrapper.eq("receiverId", userId);
        }
        queryWrapper.orderByDesc("createTime");
        Page<Message> messagePage = this.page(new Page<>(current, pageSize), queryWrapper);
        Page<MessageVO> voPage = new Page<>(current, pageSize, messagePage.getTotal());
        voPage.setRecords(buildMessageVOs(messagePage.getRecords()));
        return voPage;
    }

    @Override
    public boolean markRead(Long messageId, Long userId) {
        ThrowUtils.throwIf(messageId == null || messageId <= 0, ErrorCode.PARAMS_ERROR);
        Message message = this.getById(messageId);
        ThrowUtils.throwIf(message == null, ErrorCode.NOT_FOUND_ERROR, "消息不存在");
        ThrowUtils.throwIf(!userId.equals(message.getReceiverId()), ErrorCode.FORBIDDEN_ERROR, "只能操作自己的消息");
        if (Message.READ.equals(message.getIsRead())) {
            return true;
        }
        Message updateMessage = new Message();
        updateMessage.setId(messageId);
        updateMessage.setIsRead(Message.READ);
        return this.updateById(updateMessage);
    }

    @Override
    public boolean markAllRead(Integer type, Long friendId, Long userId) {
        MessageTypeEnum typeEnum = MessageTypeEnum.getEnumByValue(type);
        ThrowUtils.throwIf(typeEnum == null, ErrorCode.PARAMS_ERROR, "消息类型错误");
        LambdaUpdateWrapper<Message> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Message::getIsRead, Message.READ)
                .eq(Message::getType, type)
                .eq(Message::getReceiverId, userId);
        // 私信全部已读只针对与好友的会话
        if (MessageTypeEnum.PRIVATE.equals(typeEnum)) {
            ThrowUtils.throwIf(friendId == null, ErrorCode.PARAMS_ERROR, "好友 id 不能为空");
            updateWrapper.eq(Message::getSenderId, friendId);
        }
        return this.update(updateWrapper);
    }

    @Override
    public MessageUnreadVO unreadCount(Long userId) {
        MessageUnreadVO unreadVO = new MessageUnreadVO();
        // 一次分组统计两类消息的未读数量，避免两次 count 查询
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("type", "count(*) as cnt")
                .eq("receiverId", userId)
                .eq("isRead", Message.UNREAD)
                .groupBy("type");
        long systemUnread = 0;
        long privateUnread = 0;
        for (Map<String, Object> row : this.listMaps(queryWrapper)) {
            Object typeObj = row.get("type");
            Object cntObj = row.get("cnt");
            if (typeObj == null || cntObj == null) {
                continue;
            }
            int type = Integer.parseInt(typeObj.toString());
            long cnt = Long.parseLong(cntObj.toString());
            if (MessageTypeEnum.NOTIFICATION.getValue() == type) {
                systemUnread = cnt;
            } else if (MessageTypeEnum.PRIVATE.getValue() == type) {
                privateUnread = cnt;
            }
        }
        unreadVO.setSystemUnreadCount(systemUnread);
        unreadVO.setPrivateUnreadCount(privateUnread);
        return unreadVO;
    }

    @Override
    public Page<ConversationVO> listConversations(long current, long pageSize, Long userId) {
        // 1. 查询我的会话记录（按更新时间倒序分页）
        QueryWrapper<Conversation> conversationQueryWrapper = new QueryWrapper<>();
        conversationQueryWrapper.eq("userId", userId)
                .eq("isDelete", 0)
                .orderByDesc("updateTime");
        Page<Conversation> conversationPage = conversationMapper.selectPage(new Page<>(current, pageSize),
                conversationQueryWrapper);
        List<Conversation> conversationList = conversationPage.getRecords();
        Page<ConversationVO> conversationVOPage = new Page<>(conversationPage.getCurrent(),
                conversationPage.getSize(), conversationPage.getTotal());
        if (conversationList.isEmpty()) {
            conversationVOPage.setRecords(new ArrayList<>());
            return conversationVOPage;
        }
        // 2. 批量查询最后一条消息（刚创建的空会话 lastMessageId 为空）
        List<Long> lastMessageIdList = conversationList.stream()
                .map(Conversation::getLastMessageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, Message> lastMessageMap = CollectionUtils.isEmpty(lastMessageIdList) ? new HashMap<>()
                : this.listByIds(lastMessageIdList).stream()
                        .collect(Collectors.toMap(Message::getId, message -> message));
        // 3. 批量查询好友信息
        List<Long> friendIdList = conversationList.stream()
                .map(Conversation::getFriendId)
                .collect(Collectors.toList());
        Map<Long, User> friendMap = userMapper.selectBatchIds(friendIdList).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        // 4. 统计对方发给我的未读私信数量（按发送者分组）
        QueryWrapper<Message> unreadQueryWrapper = new QueryWrapper<>();
        unreadQueryWrapper.select("senderId", "count(*) as cnt")
                .eq("type", MessageTypeEnum.PRIVATE.getValue())
                .eq("receiverId", userId)
                .eq("isRead", Message.UNREAD)
                .eq("isDelete", 0)
                .groupBy("senderId");
        Map<Long, Long> unreadCountMap = new HashMap<>();
        for (Map<String, Object> row : this.listMaps(unreadQueryWrapper)) {
            Object senderIdObj = row.get("senderId");
            Object cntObj = row.get("cnt");
            if (senderIdObj != null && cntObj != null) {
                unreadCountMap.put(Long.valueOf(senderIdObj.toString()), Long.valueOf(cntObj.toString()));
            }
        }
        // 5. 组装会话视图
        List<ConversationVO> conversationVOList = new ArrayList<>(conversationList.size());
        for (Conversation conversation : conversationList) {
            Long friendId = conversation.getFriendId();
            Message lastMessage = lastMessageMap.get(conversation.getLastMessageId());
            User friend = friendMap.get(friendId);
            ConversationVO conversationVO = new ConversationVO();
            conversationVO.setFriendId(friendId);
            if (friend != null) {
                conversationVO.setFriendName(friend.getUserName());
                conversationVO.setFriendAvatar(friend.getUserAvatar());
            }
            if (lastMessage != null) {
                conversationVO.setLastContent(lastMessage.getContent());
                conversationVO.setLastTime(lastMessage.getCreateTime());
                conversationVO.setLastMessageIsMine(userId.equals(lastMessage.getSenderId()));
            }
            conversationVO.setUnreadCount(unreadCountMap.getOrDefault(friendId, 0L));
            conversationVOList.add(conversationVO);
        }
        conversationVOPage.setRecords(conversationVOList);
        return conversationVOPage;
    }

    @Override
    public ConversationVO openConversation(Long friendId, Long userId) {
        // 1. 查询对方信息
        User friend = userMapper.selectById(friendId);
        ThrowUtils.throwIf(friend == null, ErrorCode.NOT_FOUND_ERROR, "对方用户不存在");
        // 2. 会话不存在则创建（落库）
        ensureConversation(userId, friendId);
        // 3. 查询双方最后一条私信
        QueryWrapper<Message> lastQueryWrapper = new QueryWrapper<>();
        lastQueryWrapper.eq("type", MessageTypeEnum.PRIVATE.getValue())
                .eq("isDelete", 0)
                .and(q -> q.and(q2 -> q2.eq("senderId", userId).eq("receiverId", friendId))
                        .or(q3 -> q3.eq("senderId", friendId).eq("receiverId", userId)))
                .orderByDesc("id")
                .last("limit 1");
        Message lastMessage = this.getOne(lastQueryWrapper);
        // 4. 未读数与第一条未读消息 id（对方发给我的未读私信）
        QueryWrapper<Message> unreadQueryWrapper = new QueryWrapper<>();
        unreadQueryWrapper.select("count(*) as cnt", "min(id) as firstId")
                .eq("type", MessageTypeEnum.PRIVATE.getValue())
                .eq("senderId", friendId)
                .eq("receiverId", userId)
                .eq("isRead", Message.UNREAD)
                .eq("isDelete", 0);
        Map<String, Object> unreadRow = this.getMap(unreadQueryWrapper);
        long unreadCount = 0;
        Long firstUnreadId = null;
        if (unreadRow != null && unreadRow.get("cnt") != null) {
            unreadCount = Long.parseLong(unreadRow.get("cnt").toString());
            Object firstIdObj = unreadRow.get("firstId");
            if (firstIdObj != null) {
                firstUnreadId = Long.valueOf(firstIdObj.toString());
            }
        }
        // 5. 组装会话配置（返回打开前的未读状态）
        ConversationVO conversationVO = new ConversationVO();
        conversationVO.setFriendId(friend.getId());
        conversationVO.setFriendName(friend.getUserName());
        conversationVO.setFriendAvatar(friend.getUserAvatar());
        if (lastMessage != null) {
            conversationVO.setLastContent(lastMessage.getContent());
            conversationVO.setLastTime(lastMessage.getCreateTime());
            conversationVO.setLastMessageIsMine(userId.equals(lastMessage.getSenderId()));
        }
        conversationVO.setFirstUnreadId(firstUnreadId);
        // 6. 标记该好友发来的私信全部已读
        if (unreadCount > 0) {
            markAllRead(MessageTypeEnum.PRIVATE.getValue(), friendId, userId);
        }
        conversationVO.setUnreadCount(0);
        return conversationVO;
    }

    /**
     * 批量构造消息 VO（批量查询发送者信息，避免 N+1 查询）
     *
     * @param messages
     * @return
     */
    private List<MessageVO> buildMessageVOs(List<Message> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return new ArrayList<>();
        }
        // 批量查询发送者信息（系统通知 senderId 为 null）
        List<Long> senderIds = messages.stream()
                .map(Message::getSenderId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> senderMap = new HashMap<>();
        if (!senderIds.isEmpty()) {
            List<User> senderList = userMapper.selectBatchIds(senderIds);
            for (User user : senderList) {
                senderMap.put(user.getId(), user);
            }
        }
        List<MessageVO> voList = new ArrayList<>(messages.size());
        for (Message message : messages) {
            MessageVO messageVO = new MessageVO();
            BeanUtils.copyProperties(message, messageVO);
            User sender = senderMap.get(message.getSenderId());
            if (sender != null) {
                messageVO.setSenderName(sender.getUserName());
                messageVO.setSenderAvatar(sender.getUserAvatar());
            }
            voList.add(messageVO);
        }
        return voList;
    }

    /**
     * 创建或更新会话记录（更新最后一条消息，发送私信时调用）
     *
     * @param userId        会话所属用户 id
     * @param friendId      对方用户 id
     * @param lastMessageId 最后一条消息 id
     */
    private void upsertConversation(Long userId, Long friendId, Long lastMessageId) {
        Conversation exist = conversationMapper.selectOne(new QueryWrapper<Conversation>()
                .eq("userId", userId).eq("friendId", friendId).eq("isDelete", 0));
        if (exist != null) {
            Conversation updateConversation = new Conversation();
            updateConversation.setId(exist.getId());
            updateConversation.setLastMessageId(lastMessageId);
            conversationMapper.updateById(updateConversation);
            return;
        }
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setFriendId(friendId);
        conversation.setLastMessageId(lastMessageId);
        try {
            conversationMapper.insert(conversation);
        } catch (DuplicateKeyException e) {
            // 并发下唯一索引兜底：已存在则更新最后一条消息
            exist = conversationMapper.selectOne(new QueryWrapper<Conversation>()
                    .eq("userId", userId).eq("friendId", friendId).eq("isDelete", 0));
            if (exist != null) {
                Conversation updateConversation = new Conversation();
                updateConversation.setId(exist.getId());
                updateConversation.setLastMessageId(lastMessageId);
                conversationMapper.updateById(updateConversation);
            }
        }
    }

    /**
     * 确保会话记录存在（打开会话时调用，不存在则创建）
     *
     * @param userId   会话所属用户 id
     * @param friendId 对方用户 id
     */
    private void ensureConversation(Long userId, Long friendId) {
        Conversation exist = conversationMapper.selectOne(new QueryWrapper<Conversation>()
                .eq("userId", userId).eq("friendId", friendId).eq("isDelete", 0));
        if (exist != null) {
            return;
        }
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setFriendId(friendId);
        try {
            conversationMapper.insert(conversation);
        } catch (DuplicateKeyException e) {
            // 并发下已存在，忽略
        }
    }
}
