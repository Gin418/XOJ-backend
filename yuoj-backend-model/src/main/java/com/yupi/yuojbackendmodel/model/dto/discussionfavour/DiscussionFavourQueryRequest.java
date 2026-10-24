package com.yupi.yuojbackendmodel.model.dto.discussionfavour;

import com.yupi.yuojbackendcommon.common.PageRequest;
import com.yupi.yuojbackendmodel.model.dto.discussion.DiscussionQueryRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 讨论收藏查询请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DiscussionFavourQueryRequest extends PageRequest implements Serializable {

    /**
     * 讨论查询请求
     */
    private DiscussionQueryRequest discussionQueryRequest;

    /**
     * 用户 id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}
