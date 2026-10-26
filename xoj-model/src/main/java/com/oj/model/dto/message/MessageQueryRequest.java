package com.oj.model.dto.message;

import com.oj.common.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 消息查询请求
 *
 * @author Q
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MessageQueryRequest extends PageRequest implements Serializable {

    /**
     * 消息类型：0 系统通知、1 私信
     */
    private Integer type;

    /**
     * 好友用户 id（私信会话查询使用）
     */
    private Long friendId;

    private static final long serialVersionUID = 1L;
}
