package com.xoj.xojbackendcompetitionservice.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xoj.xojbackendcompetitionservice.mapper.CompetitionMapper;
import com.xoj.xojbackendcompetitionservice.mapper.UserCompetitionMapper;
import com.xoj.xojbackendcompetitionservice.service.CompetitionQuestionService;
import com.xoj.xojbackendcompetitionservice.service.CompetitionService;
import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.constant.CommonConstant;
import com.yupi.yuojbackendcommon.constant.UserConstant;
import com.yupi.yuojbackendcommon.exception.BusinessException;
import com.yupi.yuojbackendcommon.exception.ThrowUtils;
import com.yupi.yuojbackendcommon.utils.GatewayHeaderUtil;
import com.yupi.yuojbackendcommon.utils.SqlUtils;
import com.yupi.yuojbackendmodel.model.dto.competition.CompetitionQueryRequest;
import com.yupi.yuojbackendmodel.model.dto.competition.CompetitionSubmitInnerQueryRequest;
import com.yupi.yuojbackendmodel.model.dto.competition.CompetitionSwitchUpdateRequest;
import com.yupi.yuojbackendmodel.model.entity.Competition;
import com.yupi.yuojbackendmodel.model.entity.CompetitionQuestion;
import com.yupi.yuojbackendmodel.model.entity.QuestionSubmit;
import com.yupi.yuojbackendmodel.model.entity.User;
import com.yupi.yuojbackendmodel.model.entity.UserCompetition;
import com.yupi.yuojbackendmodel.model.enums.CompetitionStatusEnum;
import com.yupi.yuojbackendmodel.model.enums.JudgeInfoMessageEnum;
import com.yupi.yuojbackendmodel.model.enums.QuestionStatusEnum;
import com.yupi.yuojbackendmodel.model.vo.CompetitionDetailVO;
import com.yupi.yuojbackendmodel.model.vo.CompetitionRankItemVO;
import com.yupi.yuojbackendmodel.model.vo.CompetitionRankQuestionInfoVO;
import com.yupi.yuojbackendmodel.model.vo.CompetitionRankQuestionVO;
import com.yupi.yuojbackendmodel.model.vo.CompetitionWeekStatsVO;
import com.yupi.yuojbackendmodel.model.vo.QuestionSimpleVO;
import com.yupi.yuojbackendmodel.model.vo.UserVO;
import com.yupi.yuojbackendserviceclient.service.QuestionFeignClient;
import com.yupi.yuojbackendserviceclient.service.UserFeignClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CompetitionServiceImpl extends ServiceImpl<CompetitionMapper, Competition> implements CompetitionService {

    /**
     * 每次未通过提交的罚时：20 分钟（秒）
     */
    private static final long PENALTY_SECONDS_PER_FAIL = 20 * 60L;

    @Resource
    private CompetitionQuestionService competitionQuestionService;

    @Resource
    private UserCompetitionMapper userCompetitionMapper;

    @Resource
    private QuestionFeignClient questionFeignClient;

    @Resource
    private UserFeignClient userFeignClient;

    @Override
    public QueryWrapper<Competition> queryWrapper(CompetitionQueryRequest competitionQueryRequest) {
        QueryWrapper<Competition> queryWrapper = new QueryWrapper<>();
        if (competitionQueryRequest == null) {
            return queryWrapper;
        }
        String sortField = competitionQueryRequest.getSortField();
        String sortOrder = competitionQueryRequest.getSortOrder();
        String competitionTitle = competitionQueryRequest.getCompetitionTitle();
        String status = competitionQueryRequest.getStatus();

        queryWrapper.like(StringUtils.isNotBlank(competitionTitle), "competitionTitle", competitionTitle);
        queryWrapper.like(StringUtils.isNotBlank(status), "status", status);

        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addCompetition(Competition competition, List<Long> questionIds) {
        refreshStatus(competition);
        boolean save = this.save(competition);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
        competitionQuestionService.saveRelations(competition.getId(), questionIds);
        return competition.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCompetition(Competition competition, List<Long> questionIds) {
        refreshStatus(competition);
        boolean update = this.updateById(competition);
        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR);
        // 先删旧关系再写新关系，保证与最新题目列表一致
        competitionQuestionService.removeByCompetitionId(competition.getId());
        competitionQuestionService.saveRelations(competition.getId(), questionIds);
        return true;
    }

    @Override
    public Boolean updateCompetitionSwitch(CompetitionSwitchUpdateRequest updateRequest) {
        Long id = updateRequest.getId();
        Competition competition = this.getById(id);
        ThrowUtils.throwIf(competition == null, ErrorCode.NOT_FOUND_ERROR, "竞赛不存在");
        // 仅管理员或竞赛创建者可修改
        Long userId = GatewayHeaderUtil.getUserId();
        if (!UserConstant.ADMIN_ROLE.equals(GatewayHeaderUtil.getUserRole())
                && (userId == null || !userId.equals(competition.getUserId()))) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 只更新开关字段（updateById 忽略 null 字段）
        Competition updateCompetition = new Competition();
        updateCompetition.setId(id);
        updateCompetition.setAllowSubmitAfterEnd(updateRequest.getAllowSubmitAfterEnd());
        updateCompetition.setIsRevealed(updateRequest.getIsRevealed());
        boolean update = this.updateById(updateCompetition);
        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR);
        return true;
    }

    /**
     * 根据开始/结束时间刷新竞赛状态，创建/修改时立即生效，无需等待定时任务。
     * 前提：startTime / endTime 统一存为 yyyy-MM-dd HH:mm:ss 格式，字符串可直接按字典序比较时间先后。
     *
     * @param competition 竞赛（状态计算完成后写回 status 字段）
     */
    private void refreshStatus(Competition competition) {
        String now = DateUtil.now();
        String startTime = competition.getStartTime();
        String endTime = competition.getEndTime();
        String status;
        // 已结束：endTime 非空且已到
        if (StringUtils.isNotBlank(endTime) && endTime.compareTo(now) <= 0) {
            status = CompetitionStatusEnum.ENDED.getCode();
        } else if (StringUtils.isNotBlank(startTime) && startTime.compareTo(now) <= 0) {
            // 进行中：startTime 已到且 endTime 未到（含 endTime 为空）
            status = CompetitionStatusEnum.ONGOING.getCode();
        } else {
            // 待开始：startTime 未到（startTime 为空时默认待开始）
            status = CompetitionStatusEnum.PENDING.getCode();
        }
        competition.setStatus(status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCompetition(Long id) {
        boolean remove = this.removeById(id);
        ThrowUtils.throwIf(!remove, ErrorCode.OPERATION_ERROR);
        competitionQuestionService.removeByCompetitionId(id);
        return true;
    }

    @Override
    public CompetitionDetailVO getCompetitionDetail(Long id) {
        Competition competition = this.getById(id);
        ThrowUtils.throwIf(competition == null, ErrorCode.NOT_FOUND_ERROR, "竞赛不存在");
        // 按题号顺序（A、B、C……）提取关联题目 id 列表
        List<Long> questionIds = competitionQuestionService.listByCompetitionId(id).stream()
                .map(CompetitionQuestion::getQuestionId)
                .collect(Collectors.toList());
        CompetitionDetailVO detailVO = new CompetitionDetailVO();
        BeanUtils.copyProperties(competition, detailVO);
        detailVO.setQuestionIds(questionIds);
        return detailVO;
    }

    @Override
    public Map<Long, Integer> getQuestionStatusMap(Long competitionId, Long userId, List<Long> questionIds) {
        Map<Long, Integer> statusMap = new HashMap<>();
        if (CollectionUtils.isEmpty(questionIds)) {
            return statusMap;
        }
        // 默认全部未提交
        for (Long questionId : questionIds) {
            statusMap.put(questionId, QuestionStatusEnum.NOT_SUBMITTED.getValue());
        }
        if (userId == null || userId <= 0) {
            return statusMap;
        }
        // 当前用户在竞赛中的提交按题目分组
        List<QuestionSubmit> submitList = questionFeignClient.listCompetitionSubmitsAll(competitionId);
        Map<Long, List<QuestionSubmit>> questionSubmitMap = submitList.stream()
                .filter(submit -> userId.equals(submit.getUserId()))
                .collect(Collectors.groupingBy(QuestionSubmit::getQuestionId));
        for (Long questionId : questionIds) {
            List<QuestionSubmit> submits = questionSubmitMap.get(questionId);
            if (CollectionUtils.isEmpty(submits)) {
                continue;
            }
            boolean accepted = submits.stream()
                    .anyMatch(submit -> JudgeInfoMessageEnum.ACCEPTED.getValue().equals(submit.getStatus()));
            statusMap.put(questionId, accepted ? QuestionStatusEnum.PASSED.getValue() : QuestionStatusEnum.FAILED.getValue());
        }
        return statusMap;
    }

    @Override
    public Map<Long, UserVO> getFirstAcceptedUserVOMap(List<CompetitionQuestion> relationList) {
        if (CollectionUtils.isEmpty(relationList)) {
            return Collections.emptyMap();
        }
        Set<Long> userIds = relationList.stream()
                .map(CompetitionQuestion::getFirstAcceptedUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        // 批量查询首杀用户信息，避免 N+1
        Map<Long, User> userMap = userFeignClient.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        Map<Long, UserVO> userVOMap = new HashMap<>();
        for (Long userId : userIds) {
            User user = userMap.get(userId);
            if (user != null) {
                userVOMap.put(userId, userFeignClient.getUserVO(user));
            }
        }
        return userVOMap;
    }

    @Override
    public List<CompetitionRankQuestionInfoVO> getRankQuestionInfoList(Long competitionId) {
        List<CompetitionQuestion> relationList = competitionQuestionService.listByCompetitionId(competitionId);
        if (CollectionUtils.isEmpty(relationList)) {
            return Collections.emptyList();
        }
        // 按题号顺序（questionOrder 升序）返回
        relationList.sort(Comparator.comparing(CompetitionQuestion::getQuestionOrder));
        // 批量查询题目简信息（标题），避免 N+1
        List<Long> questionIds = relationList.stream()
                .map(CompetitionQuestion::getQuestionId)
                .collect(Collectors.toList());
        Map<Long, QuestionSimpleVO> simpleVOMap = questionFeignClient.listQuestionSimpleByIds(questionIds).stream()
                .collect(Collectors.toMap(QuestionSimpleVO::getId, simpleVO -> simpleVO, (a, b) -> a));
        return relationList.stream().map(relation -> {
            CompetitionRankQuestionInfoVO infoVO = new CompetitionRankQuestionInfoVO();
            infoVO.setQuestionId(relation.getQuestionId());
            infoVO.setCustomId(competitionQuestionService.convertToQuestionNo(relation.getQuestionOrder()));
            infoVO.setSubmitNum(relation.getSubmitNum());
            infoVO.setAcceptedNum(relation.getAcceptedNum());
            QuestionSimpleVO simpleVO = simpleVOMap.get(relation.getQuestionId());
            if (simpleVO != null) {
                infoVO.setTitle(simpleVO.getTitle());
            }
            return infoVO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<CompetitionRankItemVO> getRankList(Long competitionId, boolean realTime) {
        Competition competition = this.getById(competitionId);
        ThrowUtils.throwIf(competition == null, ErrorCode.NOT_FOUND_ERROR, "竞赛不存在");
        // 竞赛开始时间（通过秒数 = 首次通过提交时间 - 开始时间）
        Date startTime = StringUtils.isNotBlank(competition.getStartTime())
                ? DateUtil.parse(competition.getStartTime()) : null;
        // 封榜时间：未揭榜时封榜后的提交不计入排名，单独统计为冻结提交数
        Date freezeTime = StringUtils.isNotBlank(competition.getFreezeTime())
                ? DateUtil.parse(competition.getFreezeTime()) : null;
        // 结束时间：结束后的提交单独统计为赛后提交，不参与排名
        Date endTime = StringUtils.isNotBlank(competition.getEndTime())
                ? DateUtil.parse(competition.getEndTime()) : null;
        // 是否揭榜：揭榜后封榜期间的提交并入排名统计
        boolean revealed = Integer.valueOf(1).equals(competition.getIsRevealed());
        List<CompetitionQuestion> relationList = competitionQuestionService.listByCompetitionId(competitionId);
        if (CollectionUtils.isEmpty(relationList)) {
            return Collections.emptyList();
        }
        // 先从用户竞赛表查询参加竞赛的用户
        List<UserCompetition> userCompetitionList = userCompetitionMapper.selectList(
                new QueryWrapper<UserCompetition>().eq("competitionId", competitionId));
        if (CollectionUtils.isEmpty(userCompetitionList)) {
            return Collections.emptyList();
        }
        List<Long> joinUserIds = userCompetitionList.stream()
                .map(UserCompetition::getUserId)
                .collect(Collectors.toList());
        // 再根据用户 id 列表和竞赛 id 批量查询提交记录（按提交时间升序），按 用户 -> 题目 分组
        CompetitionSubmitInnerQueryRequest submitQueryRequest = new CompetitionSubmitInnerQueryRequest();
        submitQueryRequest.setCompetitionId(competitionId);
        submitQueryRequest.setUserIds(joinUserIds);
        List<QuestionSubmit> submitList = questionFeignClient.listCompetitionSubmitsByUserIds(submitQueryRequest);
        Map<Long, Map<Long, List<QuestionSubmit>>> userQuestionSubmitMap = submitList.stream()
                .collect(Collectors.groupingBy(QuestionSubmit::getUserId,
                        Collectors.groupingBy(QuestionSubmit::getQuestionId)));

        List<CompetitionRankItemVO> rankList = new ArrayList<>();
        for (Long userId : joinUserIds) {
            Map<Long, List<QuestionSubmit>> questionSubmitMap = userQuestionSubmitMap.getOrDefault(userId, Collections.emptyMap());
            CompetitionRankItemVO itemVO = new CompetitionRankItemVO();
            itemVO.setUserId(userId);
            int acceptedCount = 0;
            long penaltyTime = 0;
            // LinkedHashMap 保持题号顺序
            Map<Long, CompetitionRankQuestionVO> questionMap = new LinkedHashMap<>();
            for (CompetitionQuestion relation : relationList) {
                CompetitionRankQuestionVO questionVO = new CompetitionRankQuestionVO();
                List<QuestionSubmit> submits = questionSubmitMap.getOrDefault(
                        relation.getQuestionId(), Collections.emptyList());
                // 实时榜单：全部提交参与统计；正式榜单：只统计比赛期间的提交，未揭榜时封榜后的提交冻结不计入排名
                List<QuestionSubmit> rankedSubmits = new ArrayList<>();
                int frozenSubmitCount = 0;
                for (QuestionSubmit submit : submits) {
                    Date createTime = submit.getCreateTime();
                    if (realTime) {
                        // 实时榜单：所有提交都计入排名，不冻结
                        rankedSubmits.add(submit);
                    } else if (createTime != null && endTime != null && createTime.after(endTime)) {
                        // 正式榜单：比赛结束后的提交不计入排名
                        continue;
                    } else if (!revealed && freezeTime != null && createTime != null && createTime.after(freezeTime)) {
                        frozenSubmitCount++;
                    } else {
                        rankedSubmits.add(submit);
                    }
                }
                int submitCount = rankedSubmits.size();
                // 提交已按时间升序，第一条 Accepted 即首次通过
                QuestionSubmit firstAccepted = null;
                for (QuestionSubmit submit : rankedSubmits) {
                    if (JudgeInfoMessageEnum.ACCEPTED.getValue().equals(submit.getStatus())) {
                        firstAccepted = submit;
                        break;
                    }
                }
                boolean accepted = firstAccepted != null;
                questionVO.setAccepted(accepted);
                long acceptTimeSeconds = -1;
                if (accepted) {
                    acceptedCount++;
                    if (startTime != null) {
                        acceptTimeSeconds = (firstAccepted.getCreateTime().getTime() - startTime.getTime()) / 1000;
                    }
                    questionVO.setFirstBlood(userId.equals(relation.getFirstAcceptedUserId()));
                    // 罚时：首次通过前的失败提交数 × 20 分钟 + 通过记录与竞赛开始的秒差（通过后的提交不罚）
                    int failCountBeforeAccept = rankedSubmits.indexOf(firstAccepted);
                    // 提交次数只计到首次通过为止（通过后的提交不计入）
                    submitCount = failCountBeforeAccept + 1;
                    questionVO.setSubmitCount(submitCount);
                    if (firstAccepted.getCreateTime() != null && endTime != null
                            && firstAccepted.getCreateTime().after(endTime)) {
                        long postContestAcceptSeconds = (firstAccepted.getCreateTime().getTime() - startTime.getTime()) / 1000;
                        questionVO.setPostContestAcceptSeconds(postContestAcceptSeconds);
                        questionVO.setPostContestAccepted(true);
                    }
                    penaltyTime += (long) failCountBeforeAccept * PENALTY_SECONDS_PER_FAIL + acceptTimeSeconds;
                } else {
                    questionVO.setFirstBlood(false);
                    // 未通过：全部提交都算罚时
                    penaltyTime += (long) submitCount * PENALTY_SECONDS_PER_FAIL;
                    questionVO.setSubmitCount(submitCount);
                }
                questionVO.setAcceptTimeSeconds(acceptTimeSeconds);
                // 封榜后的提交数（未揭榜时冻结，不参与排名统计）
                questionVO.setFrozenSubmitCount(frozenSubmitCount);
                questionMap.put(relation.getQuestionId(), questionVO);
            }
            itemVO.setQuestions(questionMap);
            itemVO.setAcceptedCount(acceptedCount);
            itemVO.setPenaltyTime(penaltyTime);
            rankList.add(itemVO);
        }
        // 排序：通过数降序、罚时升序、用户 id 升序（保证顺序稳定）
        rankList.sort(Comparator.comparing(CompetitionRankItemVO::getAcceptedCount).reversed()
                .thenComparing(CompetitionRankItemVO::getPenaltyTime)
                .thenComparing(CompetitionRankItemVO::getUserId));
        // 填充排名与用户信息
        List<Long> userIds = rankList.stream().map(CompetitionRankItemVO::getUserId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(userIds)) {
            return rankList;
        }
        Map<Long, User> userMap = userFeignClient.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        for (int i = 0; i < rankList.size(); i++) {
            CompetitionRankItemVO itemVO = rankList.get(i);
            itemVO.setRank(i + 1);
            itemVO.setUserVO(userFeignClient.getUserVO(userMap.get(itemVO.getUserId())));
        }
        return rankList;
    }

    @Override
    public CompetitionWeekStatsVO getWeekStats() {
        // 本周一 00:00:00 ~ 下周一 00:00:00（不含）
        LocalDateTime weekStart = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime weekEnd = weekStart.plusWeeks(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTime = weekStart.format(formatter);
        String endTime = weekEnd.format(formatter);
        // startTime 落在本周内的竞赛（字符串按字典序比较时间先后）
        QueryWrapper<Competition> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("startTime", startTime);
        queryWrapper.lt("startTime", endTime);
        queryWrapper.eq("isDelete", false);
        List<Competition> competitionList = this.list(queryWrapper);
        // 按状态统计
        int pendingCount = 0;
        int ongoingCount = 0;
        for (Competition competition : competitionList) {
            if (CompetitionStatusEnum.PENDING.getCode().equals(competition.getStatus())) {
                pendingCount++;
            } else if (CompetitionStatusEnum.ONGOING.getCode().equals(competition.getStatus())) {
                ongoingCount++;
            }
        }
        CompetitionWeekStatsVO statsVO = new CompetitionWeekStatsVO();
        statsVO.setAllCount(competitionList.size());
        statsVO.setPendingCount(pendingCount);
        statsVO.setOngoingCount(ongoingCount);
        return statsVO;
    }

    @Override
    public Page<CompetitionRankItemVO> getRankPage(Long competitionId, long current, long pageSize, boolean realTime) {
        // 排名需全量统计并排序后才能确定名次，排序完成后按页截取
        List<CompetitionRankItemVO> rankList = getRankList(competitionId, realTime);
        Page<CompetitionRankItemVO> page = new Page<>(Math.max(current, 1), Math.max(pageSize, 1));
        page.setTotal(rankList.size());
        int fromIndex = (int) Math.min((page.getCurrent() - 1) * page.getSize(), rankList.size());
        int toIndex = (int) Math.min(fromIndex + page.getSize(), rankList.size());
        page.setRecords(rankList.subList(fromIndex, toIndex));
        return page;
    }
}
