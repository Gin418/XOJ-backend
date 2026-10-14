package com.xoj.xojbackendcompetitionservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yuojbackendmodel.model.dto.competition.CompetitionQueryRequest;
import com.yupi.yuojbackendmodel.model.entity.Competition;
import com.yupi.yuojbackendmodel.model.vo.CompetitionRankItemVO;

import java.util.List;

public interface CompetitionService extends IService<Competition> {

    QueryWrapper<Competition> queryWrapper(CompetitionQueryRequest competitionQueryRequest);

    /**
     * 新增竞赛并保存题目关系
     *
     * @param competition 竞赛实体
     * @param questionIds 题目 id 列表
     * @return 竞赛 id
     */
    Long addCompetition(Competition competition, List<Long> questionIds);

    /**
     * 修改竞赛并刷新题目关系（先删旧关系再写新关系）
     *
     * @param competition 竞赛实体
     * @param questionIds 题目 id 列表
     * @return 是否成功
     */
    Boolean updateCompetition(Competition competition, List<Long> questionIds);

    /**
     * 删除竞赛及其题目关系
     *
     * @param id 竞赛 id
     * @return 是否成功
     */
    Boolean deleteCompetition(Long id);

    /**
     * 查询竞赛排名（按通过数降序、罚时升序）
     *
     * @param competitionId 竞赛 id
     * @return 排名列表
     */
    List<CompetitionRankItemVO> getRankList(Long competitionId);
}
