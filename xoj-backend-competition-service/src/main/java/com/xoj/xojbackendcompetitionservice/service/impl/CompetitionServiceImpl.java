package com.xoj.xojbackendcompetitionservice.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xoj.xojbackendcompetitionservice.mapper.CompetitionMapper;
import com.xoj.xojbackendcompetitionservice.service.CompetitionQuestionService;
import com.xoj.xojbackendcompetitionservice.service.CompetitionService;
import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.constant.CommonConstant;
import com.yupi.yuojbackendcommon.exception.ThrowUtils;
import com.yupi.yuojbackendcommon.utils.SqlUtils;
import com.yupi.yuojbackendmodel.model.dto.competition.CompetitionQueryRequest;
import com.yupi.yuojbackendmodel.model.entity.Competition;
import com.yupi.yuojbackendmodel.model.entity.CompetitionQuestion;
import com.yupi.yuojbackendmodel.model.entity.QuestionSubmit;
import com.yupi.yuojbackendmodel.model.entity.User;
import com.yupi.yuojbackendmodel.model.enums.JudgeInfoMessageEnum;
import com.yupi.yuojbackendmodel.model.vo.CompetitionRankItemVO;
import com.yupi.yuojbackendmodel.model.vo.CompetitionRankQuestionVO;
import com.yupi.yuojbackendserviceclient.service.QuestionFeignClient;
import com.yupi.yuojbackendserviceclient.service.UserFeignClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        boolean save = this.save(competition);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
        competitionQuestionService.saveRelations(competition.getId(), questionIds);
        return competition.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCompetition(Competition competition, List<Long> questionIds) {
        boolean update = this.updateById(competition);
        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR);
        // 先删旧关系再写新关系，保证与最新题目列表一致
        competitionQuestionService.removeByCompetitionId(competition.getId());
        competitionQuestionService.saveRelations(competition.getId(), questionIds);
        return true;
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
    public List<CompetitionRankItemVO> getRankList(Long competitionId) {
        Competition competition = this.getById(competitionId);
        ThrowUtils.throwIf(competition == null, ErrorCode.NOT_FOUND_ERROR, "竞赛不存在");
        // 竞赛开始时间（通过秒数 = 首次通过提交时间 - 开始时间）
        Date startTime = StringUtils.isNotBlank(competition.getStartTime())
                ? DateUtil.parse(competition.getStartTime()) : null;
        List<CompetitionQuestion> relationList = competitionQuestionService.listByCompetitionId(competitionId);
        if (CollectionUtils.isEmpty(relationList)) {
            return Collections.emptyList();
        }
        // 拉取竞赛全部提交（按提交时间升序），按 用户 -> 题目 分组
        List<QuestionSubmit> submitList = questionFeignClient.listCompetitionSubmitsAll(competitionId);
        Map<Long, Map<Long, List<QuestionSubmit>>> userQuestionSubmitMap = submitList.stream()
                .collect(Collectors.groupingBy(QuestionSubmit::getUserId,
                        Collectors.groupingBy(QuestionSubmit::getQuestionId)));

        List<CompetitionRankItemVO> rankList = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, List<QuestionSubmit>>> entry : userQuestionSubmitMap.entrySet()) {
            Long userId = entry.getKey();
            Map<Long, List<QuestionSubmit>> questionSubmitMap = entry.getValue();
            CompetitionRankItemVO itemVO = new CompetitionRankItemVO();
            itemVO.setUserId(userId);
            int acceptedCount = 0;
            long penaltyTime = 0;
            // LinkedHashMap 保持题号顺序
            Map<Long, CompetitionRankQuestionVO> questionMap = new LinkedHashMap<>();
            for (CompetitionQuestion relation : relationList) {
                CompetitionRankQuestionVO questionVO = new CompetitionRankQuestionVO();
                List<QuestionSubmit> submits = questionSubmitMap.get(relation.getQuestionId());
                int submitCount = submits == null ? 0 : submits.size();
                questionVO.setSubmitCount(submitCount);
                // 提交已按时间升序，第一条 Accepted 即首次通过
                QuestionSubmit firstAccepted = null;
                if (submits != null) {
                    for (QuestionSubmit submit : submits) {
                        if (JudgeInfoMessageEnum.ACCEPTED.getValue().equals(submit.getStatus())) {
                            firstAccepted = submit;
                            break;
                        }
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
                    int failCountBeforeAccept = submits.indexOf(firstAccepted);
                    penaltyTime += (long) failCountBeforeAccept * PENALTY_SECONDS_PER_FAIL + acceptTimeSeconds;
                } else {
                    questionVO.setFirstBlood(false);
                    // 未通过：全部提交都算罚时
                    penaltyTime += (long) submitCount * PENALTY_SECONDS_PER_FAIL;
                }
                questionVO.setAcceptTimeSeconds(acceptTimeSeconds);
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
        Map<Long, User> userMap = userFeignClient.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        for (int i = 0; i < rankList.size(); i++) {
            CompetitionRankItemVO itemVO = rankList.get(i);
            itemVO.setRank(i + 1);
            itemVO.setUserVO(userFeignClient.getUserVO(userMap.get(itemVO.getUserId())));
        }
        return rankList;
    }
}
