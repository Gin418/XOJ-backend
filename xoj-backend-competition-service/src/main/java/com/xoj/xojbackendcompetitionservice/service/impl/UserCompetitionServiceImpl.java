package com.xoj.xojbackendcompetitionservice.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xoj.xojbackendcompetitionservice.mapper.UserCompetitionMapper;
import com.xoj.xojbackendcompetitionservice.service.CompetitionService;
import com.xoj.xojbackendcompetitionservice.service.UserCompetitionService;
import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.exception.ThrowUtils;
import com.yupi.yuojbackendmodel.model.entity.Competition;
import com.yupi.yuojbackendmodel.model.entity.UserCompetition;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class UserCompetitionServiceImpl extends ServiceImpl<UserCompetitionMapper, UserCompetition> implements UserCompetitionService {

    @Resource
    private CompetitionService competitionService;

    @Override
    public Long joinCompetition(Long competitionId, Long userId) {
        // 校验竞赛存在
        Competition competition = competitionService.getById(competitionId);
        ThrowUtils.throwIf(competition == null, ErrorCode.NOT_FOUND_ERROR, "竞赛不存在");
        // 校验竞赛未结束（只要未结束即可报名）
        ThrowUtils.throwIf(StringUtils.isNotBlank(competition.getEndTime())
                        && DateUtil.parse(competition.getEndTime()).isBefore(new Date()),
                ErrorCode.OPERATION_ERROR, "竞赛已结束，无法报名");
        // 校验用户未重复参加
        QueryWrapper<UserCompetition> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("competitionId", competitionId);
        queryWrapper.eq("userId", userId);
        long count = this.count(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.OPERATION_ERROR, "已参加该竞赛");
        // 新增参加记录
        UserCompetition userCompetition = new UserCompetition();
        userCompetition.setCompetitionId(competitionId);
        userCompetition.setUserId(userId);
        userCompetition.setPenaltyTime(0L);
        boolean save = this.save(userCompetition);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
        return userCompetition.getId();
    }
}
