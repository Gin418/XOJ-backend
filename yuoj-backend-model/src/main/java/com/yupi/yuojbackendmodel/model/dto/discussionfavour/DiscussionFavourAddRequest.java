package com.yupi.yuojbackendmodel.model.dto.discussionfavour;

import lombok.Data;

import java.io.Serializable;

/**
 * 讨论收藏 / 取消收藏请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class DiscussionFavourAddRequest implements Serializable {

    /**
     * 讨论 id
     */
    private Long discussionId;

    private static final long serialVersionUID = 1L;
}
