package com.oj.competitionservice.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oj.competitionservice.mapper.UserCompetitionMapper;
import com.oj.competitionservice.service.CompetitionService;
import com.oj.competitionservice.service.UserCompetitionService;
import com.oj.common.common.ErrorCode;
import com.oj.common.exception.ThrowUtils;
import com.oj.model.entity.Competition;
import com.oj.model.entity.UserCompetition;
import com.oj.model.vo.CompetitionVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        ThrowUtils.throwIf(this.hasJoined(competitionId, userId), ErrorCode.OPERATION_ERROR, "已参加该竞赛");
        // 新增参加记录
        UserCompetition userCompetition = new UserCompetition();
        userCompetition.setCompetitionId(competitionId);
        userCompetition.setUserId(userId);
        userCompetition.setPenaltyTime(0L);
        boolean save = this.save(userCompetition);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
        return userCompetition.getId();
    }

    @Override
    public boolean hasJoined(Long competitionId, Long userId) {
        QueryWrapper<UserCompetition> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("competitionId", competitionId);
        queryWrapper.eq("userId", userId);
        return this.count(queryWrapper) > 0;
    }

    @Override
    public Page<CompetitionVO> listMyCompetitionsPage(Long userId, long current, long pageSize) {
        // 分页查询用户参加的竞赛关系（按参加时间倒序）
        QueryWrapper<UserCompetition> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.orderByDesc("createTime");
        Page<UserCompetition> relationPage = this.page(new Page<>(current, pageSize), queryWrapper);
        List<UserCompetition> relationList = relationPage.getRecords();
        Page<CompetitionVO> competitionVOPage = new Page<>(relationPage.getCurrent(), relationPage.getSize(), relationPage.getTotal());
        if (CollectionUtils.isEmpty(relationList)) {
            return competitionVOPage;
        }
        // 批量查询竞赛信息，避免 N+1
        List<Long> competitionIds = relationList.stream()
                .map(UserCompetition::getCompetitionId)
                .collect(Collectors.toList());
        Map<Long, Competition> competitionMap = competitionService.listByIds(competitionIds).stream()
                .collect(Collectors.toMap(Competition::getId, competition -> competition, (a, b) -> a));
        // 按参加时间倒序组装 VO
        List<CompetitionVO> competitionVOList = relationList.stream()
                .map(relation -> CompetitionVO.objToVo(competitionMap.get(relation.getCompetitionId())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        competitionVOPage.setRecords(competitionVOList);
        return competitionVOPage;
    }
}
