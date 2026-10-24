package com.yupi.yuojbackendmodel.model.dto.message;

import lombok.Data;

import java.io.Serializable;

/**
 * 打开会话请求（标记已读并返回会话配置）
 *
 * @author Q
 */
@Data
public class ConversationOpenRequest implements Serializable {

    /**
     * 好友用户 id
     */
    private Long friendId;

    private static final long serialVersionUID = 1L;
}
