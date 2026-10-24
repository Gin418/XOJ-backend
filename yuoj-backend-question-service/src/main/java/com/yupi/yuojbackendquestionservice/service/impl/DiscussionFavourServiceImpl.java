package com.yupi.yuojbackendquestionservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.exception.BusinessException;
import com.yupi.yuojbackendmodel.model.entity.Discussion;
import com.yupi.yuojbackendmodel.model.entity.DiscussionFavour;
import com.yupi.yuojbackendquestionservice.mapper.DiscussionFavourMapper;
import com.yupi.yuojbackendquestionservice.service.DiscussionFavourService;
import com.yupi.yuojbackendquestionservice.service.DiscussionService;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 讨论收藏服务实现
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Service
public class DiscussionFavourServiceImpl extends ServiceImpl<DiscussionFavourMapper, DiscussionFavour>
        implements DiscussionFavourService {

    @Resource
    private DiscussionService discussionService;

    /**
     * 讨论收藏
     *
     * @param discussionId
     * @param userId
     * @return
     */
    @Override
    public int doDiscussionFavour(long discussionId, long userId) {
        // 判断是否存在
        Discussion discussion = discussionService.getById(discussionId);
        if (discussion == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 每个用户串行讨论收藏
        // 锁必须要包裹住事务方法
        DiscussionFavourService discussionFavourService = (DiscussionFavourService) AopContext.currentProxy();
        synchronized (String.valueOf(userId).intern()) {
            return discussionFavourService.doDiscussionFavourInner(userId, discussionId);
        }
    }

    @Override
    public Page<Discussion> listFavourDiscussionByPage(IPage<Discussion> page, Wrapper<Discussion> queryWrapper, long favourUserId) {
        if (favourUserId <= 0) {
            return new Page<>();
        }
        return baseMapper.listFavourDiscussionByPage(page, queryWrapper, favourUserId);
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
    public int doDiscussionFavourInner(long userId, long discussionId) {
        DiscussionFavour discussionFavour = new DiscussionFavour();
        discussionFavour.setUserId(userId);
        discussionFavour.setDiscussionId(discussionId);
        QueryWrapper<DiscussionFavour> discussionFavourQueryWrapper = new QueryWrapper<>(discussionFavour);
        DiscussionFavour oldDiscussionFavour = this.getOne(discussionFavourQueryWrapper);
        boolean result;
        // 已收藏
        if (oldDiscussionFavour != null) {
            result = this.remove(discussionFavourQueryWrapper);
            if (result) {
                // 讨论收藏数 - 1
                result = discussionService.update()
                        .eq("id", discussionId)
                        .gt("favourNum", 0)
                        .setSql("favourNum = favourNum - 1")
                        .update();
                return result ? -1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        } else {
            // 未讨论收藏
            result = this.save(discussionFavour);
            if (result) {
                // 讨论收藏数 + 1
                result = discussionService.update()
                        .eq("id", discussionId)
                        .setSql("favourNum = favourNum + 1")
                        .update();
                return result ? 1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        }
    }

}
