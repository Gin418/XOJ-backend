package com.xoj.xojbackendcompetitionservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yuojbackendmodel.model.entity.UserCompetition;

public interface UserCompetitionService extends IService<UserCompetition> {

    /**
     * 用户参加竞赛
     *
     * @param competitionId 竞赛 id
     * @param userId        用户 id
     * @return 关系记录 id
     */
    Long joinCompetition(Long competitionId, Long userId);
}
