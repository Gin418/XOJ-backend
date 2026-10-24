package com.yupi.yuojbackendquestionservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yuojbackendmodel.model.entity.DiscussionThumb;

/**
 * 讨论点赞服务
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public interface DiscussionThumbService extends IService<DiscussionThumb> {

    /**
     * 点赞
     *
     * @param discussionId
     * @param userId
     * @return
     */
    int doDiscussionThumb(long discussionId, long userId);

    /**
     * 讨论点赞（内部服务）
     *
     * @param userId
     * @param discussionId
     * @return
     */
    int doDiscussionThumbInner(long userId, long discussionId);
}
