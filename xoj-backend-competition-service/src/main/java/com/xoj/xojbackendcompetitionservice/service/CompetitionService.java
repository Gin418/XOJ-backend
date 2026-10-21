package com.xoj.xojbackendcompetitionservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yuojbackendmodel.model.dto.competition.CompetitionQueryRequest;
import com.yupi.yuojbackendmodel.model.dto.competition.CompetitionSwitchUpdateRequest;
import com.yupi.yuojbackendmodel.model.entity.Competition;
import com.yupi.yuojbackendmodel.model.entity.CompetitionQuestion;
import com.yupi.yuojbackendmodel.model.vo.CompetitionDetailVO;
import com.yupi.yuojbackendmodel.model.vo.CompetitionRankItemVO;
import com.yupi.yuojbackendmodel.model.vo.CompetitionRankQuestionInfoVO;
import com.yupi.yuojbackendmodel.model.vo.CompetitionWeekStatsVO;
import com.yupi.yuojbackendmodel.model.vo.UserVO;

import java.util.List;
import java.util.Map;

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
     * 修改竞赛开关（是否允许赛后提交、是否揭榜），仅管理员或竞赛创建者可修改
     *
     * @param updateRequest 更新请求
     * @return 是否成功
     */
    Boolean updateCompetitionSwitch(CompetitionSwitchUpdateRequest updateRequest);

    /**
     * 删除竞赛及其题目关系
     *
     * @param id 竞赛 id
     * @return 是否成功
     */
    Boolean deleteCompetition(Long id);

    /**
     * 查询竞赛详情（竞赛全部信息 + 关联题目 id 列表）
     *
     * @param id 竞赛 id
     * @return 竞赛详情 VO
     */
    CompetitionDetailVO getCompetitionDetail(Long id);

    /**
     * 查询竞赛排名（按通过数降序、罚时升序）
     *
     * @param competitionId 竞赛 id
     * @param realTime      是否实时榜单（true 查全部提交、false 正式榜单只查比赛期间的提交）
     * @return 排名列表
     */
    List<CompetitionRankItemVO> getRankList(Long competitionId, boolean realTime);

    /**
     * 分页查询竞赛排名（按通过数降序、罚时升序）
     *
     * @param competitionId 竞赛 id
     * @param current       当前页
     * @param pageSize      页大小
     * @param realTime      是否实时榜单（true 查全部提交、false 正式榜单只查比赛期间的提交）
     * @return 排名分页结果
     */
    Page<CompetitionRankItemVO> getRankPage(Long competitionId, long current, long pageSize, boolean realTime);

    /**
     * 查询用户在竞赛中各题的提交情况（0 未提交、1 提交未通过、2 通过）
     *
     * @param competitionId 竞赛 id
     * @param userId        用户 id
     * @param questionIds   题目 id 列表
     * @return 题目 id -> 提交情况
     */
    Map<Long, Integer> getQuestionStatusMap(Long competitionId, Long userId, List<Long> questionIds);

    /**
     * 查询题目关系列表中各题首杀用户信息
     *
     * @param relationList 题目关系列表
     * @return 用户 id -> 用户 VO（脱敏）
     */
    Map<Long, UserVO> getFirstAcceptedUserVOMap(List<CompetitionQuestion> relationList);

    /**
     * 查询竞赛排名页题目统计（题号、标题、提交数、通过数，按题号顺序）
     *
     * @param competitionId 竞赛 id
     * @return 题目统计列表
     */
    List<CompetitionRankQuestionInfoVO> getRankQuestionInfoList(Long competitionId);

    /**
     * 查询本周竞赛统计（总数、未开始、进行中）
     *
     * @return 本周竞赛统计 VO
     */
    CompetitionWeekStatsVO getWeekStats();
}
