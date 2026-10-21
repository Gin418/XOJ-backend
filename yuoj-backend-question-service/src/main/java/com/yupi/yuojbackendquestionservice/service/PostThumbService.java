package com.yupi.yuojbackendquestionservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yuojbackendmodel.model.entity.PostThumb;

/**
 * 帖子点赞服务
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public interface PostThumbService extends IService<PostThumb> {

    /**
     * 点赞
     *
     * @param postId
     * @param userId
     * @return
     */
    int doPostThumb(long postId, long userId);

    /**
     * 帖子点赞（内部服务）
     *
     * @param userId
     * @param postId
     * @return
     */
    int doPostThumbInner(long userId, long postId);
}
