package com.xoj.xojbackendcompetitionservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xoj.xojbackendcompetitionservice.mapper.CompetitionQuestionMapper;
import com.xoj.xojbackendcompetitionservice.service.CompetitionQuestionService;
import com.yupi.yuojbackendmodel.model.entity.CompetitionQuestion;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompetitionQuestionServiceImpl extends ServiceImpl<CompetitionQuestionMapper, CompetitionQuestion> implements CompetitionQuestionService {

    @Override
    public void saveRelations(Long competitionId, List<Long> questionIds) {
        if (competitionId == null || CollectionUtils.isEmpty(questionIds)) {
            return;
        }
        // 去重（保持传入顺序），列表顺序即题号顺序（A、B、C、D……）
        List<Long> distinctQuestionIds = questionIds.stream().distinct().collect(Collectors.toList());
        List<CompetitionQuestion> relationList = new ArrayList<>(distinctQuestionIds.size());
        int questionOrder = 1;
        for (Long questionId : distinctQuestionIds) {
            CompetitionQuestion relation = new CompetitionQuestion();
            relation.setCompetitionId(competitionId);
            relation.setQuestionId(questionId);
            relation.setQuestionOrder(questionOrder++);
            relationList.add(relation);
        }
        this.saveBatch(relationList);
    }

    @Override
    public void removeByCompetitionId(Long competitionId) {
        if (competitionId == null) {
            return;
        }
        QueryWrapper<CompetitionQuestion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("competitionId", competitionId);
        this.remove(queryWrapper);
    }

    @Override
    public List<CompetitionQuestion> listByCompetitionId(Long competitionId) {
        if (competitionId == null) {
            return Collections.emptyList();
        }
        QueryWrapper<CompetitionQuestion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("competitionId", competitionId);
        queryWrapper.orderByAsc("questionOrder");
        return this.list(queryWrapper);
    }

    @Override
    public CompetitionQuestion getByCompetitionIdAndOrder(Long competitionId, Integer questionOrder) {
        if (competitionId == null || questionOrder == null) {
            return null;
        }
        QueryWrapper<CompetitionQuestion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("competitionId", competitionId);
        queryWrapper.eq("questionOrder", questionOrder);
        return this.getOne(queryWrapper);
    }

    @Override
    public boolean updateSubmitNum(long competitionId, long questionId) {
        UpdateWrapper<CompetitionQuestion> updateWrapper = new UpdateWrapper<>();
        updateWrapper.setSql("submitNum = submitNum + 1")
                .eq("competitionId", competitionId)
                .eq("questionId", questionId);
        return this.update(updateWrapper);
    }

    @Override
    public boolean updateAcceptNum(long competitionId, long questionId, long userId, String acceptTime) {
        UpdateWrapper<CompetitionQuestion> updateWrapper = new UpdateWrapper<>();
        // CAS：只有提交时间更早（或当前无首杀）才替换首杀，保证结果与到达顺序无关
        // 注：acceptTime 为内部生成的 yyyy-MM-dd HH:mm:ss 格式字符串，userId 为 long，无注入风险
        String sql = "acceptedNum = acceptedNum + 1, "
                + "firstAcceptedUserId = CASE WHEN firstAcceptedTime IS NULL OR firstAcceptedTime > '" + acceptTime + "' THEN " + userId + " ELSE firstAcceptedUserId END, "
                + "firstAcceptedTime = CASE WHEN firstAcceptedTime IS NULL OR firstAcceptedTime > '" + acceptTime + "' THEN '" + acceptTime + "' ELSE firstAcceptedTime END";
        updateWrapper.setSql(sql)
                .eq("competitionId", competitionId)
                .eq("questionId", questionId);
        return this.update(updateWrapper);
    }
}
