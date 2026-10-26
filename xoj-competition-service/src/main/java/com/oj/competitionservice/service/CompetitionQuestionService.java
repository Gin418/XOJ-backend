package com.oj.competitionservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oj.model.entity.CompetitionQuestion;

import java.util.List;

public interface CompetitionQuestionService extends IService<CompetitionQuestion> {

    /**
     * 题目顺序转题号：1→A，2→B……26→Z，27→AA，28→AB……（Excel 列名规则）
     */
    default String convertToQuestionNo(Integer questionOrder) {
        if (questionOrder == null || questionOrder < 1) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int n = questionOrder;
        while (n > 0) {
            n--;
            sb.append((char) ('A' + n % 26));
            n /= 26;
        }
        return sb.reverse().toString();
    }

    /**
     * 题号转题目顺序：A→1，B→2……AA→27（Excel 列名规则），数字题号原样返回
     */
    default Integer convertToQuestionOrder(String customId) {
        if (customId == null) {
            return null;
        }
        String upper = customId.trim().toUpperCase();
        if (upper.isEmpty()) {
            return null;
        }
        boolean allLetters = true;
        for (char c : upper.toCharArray()) {
            if (c < 'A' || c > 'Z') {
                allLetters = false;
                break;
            }
        }
        if (allLetters) {
            int order = 0;
            for (char c : upper.toCharArray()) {
                order = order * 26 + (c - 'A' + 1);
            }
            return order;
        }
        try {
            int order = Integer.parseInt(upper);
            return order >= 1 ? order : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 保存竞赛与题目的关系（批量新增）
     *
     * @param competitionId 竞赛 id
     * @param questionIds   题目 id 列表
     */
    void saveRelations(Long competitionId, List<Long> questionIds);

    /**
     * 删除竞赛的全部题目关系
     *
     * @param competitionId 竞赛 id
     */
    void removeByCompetitionId(Long competitionId);

    /**
     * 按题号顺序查询竞赛的题目关系列表（questionOrder 升序，即 A、B、C、D……）
     *
     * @param competitionId 竞赛 id
     * @return 关系列表
     */
    List<CompetitionQuestion> listByCompetitionId(Long competitionId);

    /**
     * 按竞赛 id 和题号顺序查询单个题目关系
     *
     * @param competitionId 竞赛 id
     * @param questionOrder 题目顺序（1=A，2=B……）
     * @return 关系记录
     */
    CompetitionQuestion getByCompetitionIdAndOrder(Long competitionId, Integer questionOrder);

    /**
     * 竞赛题目提交数 +1（原子更新，防止并发下统计失真）
     *
     * @param competitionId 竞赛 id
     * @param questionId    题目 id
     * @return 是否更新成功
     */
    boolean updateSubmitNum(long competitionId, long questionId);

    /**
     * 竞赛题目通过数 +1，并按提交时间记录第一个通过的用户（CAS 保证提交最早者为首杀）
     *
     * @param competitionId 竞赛 id
     * @param questionId    题目 id
     * @param userId        通过的用户 id
     * @param acceptTime    通过用户的提交时间（yyyy-MM-dd HH:mm:ss）
     * @return 是否更新成功
     */
    boolean updateAcceptNum(long competitionId, long questionId, long userId, String acceptTime);
}
