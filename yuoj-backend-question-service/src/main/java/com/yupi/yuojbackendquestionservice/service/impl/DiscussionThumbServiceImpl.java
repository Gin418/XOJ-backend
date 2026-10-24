package com.yupi.yuojbackendquestionservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.exception.BusinessException;
import com.yupi.yuojbackendmodel.model.entity.Discussion;
import com.yupi.yuojbackendmodel.model.entity.DiscussionThumb;
import com.yupi.yuojbackendquestionservice.mapper.DiscussionThumbMapper;
import com.yupi.yuojbackendquestionservice.service.DiscussionService;
import com.yupi.yuojbackendquestionservice.service.DiscussionThumbService;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 讨论点赞服务实现
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Service
public class DiscussionThumbServiceImpl extends ServiceImpl<DiscussionThumbMapper, DiscussionThumb>
        implements DiscussionThumbService {

    @Resource
    private DiscussionService discussionService;

    /**
     * 点赞
     *
     * @param discussionId
     * @param userId
     * @return
     */
    @Override
    public int doDiscussionThumb(long discussionId, long userId) {
        // 判断实体是否存在，根据类别获取实体
        Discussion discussion = discussionService.getById(discussionId);
        if (discussion == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 每个用户串行点赞
        // 锁必须要包裹住事务方法
        DiscussionThumbService discussionThumbService = (DiscussionThumbService) AopContext.currentProxy();
        synchronized (String.valueOf(userId).intern()) {
            return discussionThumbService.doDiscussionThumbInner(userId, discussionId);
        }
    }

    /**
     * 封装了事务的方法
     *
     * @param userId
     * @param discussionId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int doDiscussionThumbInner(long userId, long discussionId) {
        DiscussionThumb discussionThumb = new DiscussionThumb();
        discussionThumb.setUserId(userId);
        discussionThumb.setDiscussionId(discussionId);
        QueryWrapper<DiscussionThumb> thumbQueryWrapper = new QueryWrapper<>(discussionThumb);
        DiscussionThumb oldDiscussionThumb = this.getOne(thumbQueryWrapper);
        boolean result;
        // 已点赞
        if (oldDiscussionThumb != null) {
            result = this.remove(thumbQueryWrapper);
            if (result) {
                // 点赞数 - 1
                result = discussionService.update()
                        .eq("id", discussionId)
                        .gt("thumbNum", 0)
                        .setSql("thumbNum = thumbNum - 1")
                        .update();
                return result ? -1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        } else {
            // 未点赞
            result = this.save(discussionThumb);
            if (result) {
                // 点赞数 + 1
                result = discussionService.update()
                        .eq("id", discussionId)
                        .setSql("thumbNum = thumbNum + 1")
                        .update();
                return result ? 1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        }
    }

}
