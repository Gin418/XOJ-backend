package com.oj.competitionservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.oj.model.entity.UserCompetition;
import com.oj.model.vo.CompetitionVO;

public interface UserCompetitionService extends IService<UserCompetition> {

    /**
     * 用户参加竞赛
     *
     * @param competitionId 竞赛 id
     * @param userId        用户 id
     * @return 关系记录 id
     */
    Long joinCompetition(Long competitionId, Long userId);

    /**
     * 查询用户是否已参加竞赛
     *
     * @param competitionId 竞赛 id
     * @param userId        用户 id
     * @return 是否已参加
     */
    boolean hasJoined(Long competitionId, Long userId);

    /**
     * 分页查询当前用户参加的竞赛列表（按参加时间倒序）
     *
     * @param userId   用户 id
     * @param current  当前页
     * @param pageSize 每页条数
     * @return 竞赛 VO 分页
     */
    Page<CompetitionVO> listMyCompetitionsPage(Long userId, long current, long pageSize);
}
