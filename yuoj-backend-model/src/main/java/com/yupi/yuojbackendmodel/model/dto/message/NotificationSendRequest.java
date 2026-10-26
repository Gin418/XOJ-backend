package com.yupi.yuojbackendmodel.model.dto.message;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 系统通知发送请求
 *
 * @author Q
 */
@Data
public class NotificationSendRequest implements Serializable {

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 接收用户 id 列表（为空则广播给所有在线用户，广播不落库）
     */
    private List<Long> receiverIds;

    private static final long serialVersionUID = 1L;
}
