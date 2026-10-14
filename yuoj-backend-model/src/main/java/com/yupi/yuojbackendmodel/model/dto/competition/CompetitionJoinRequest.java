package com.yupi.yuojbackendmodel.model.dto.competition;

import lombok.Data;

import java.io.Serializable;

/**
 * 参加竞赛请求
 */
@Data
public class CompetitionJoinRequest implements Serializable {

    /**
     * 竞赛 id
     */
    private Long competitionId;

    private static final long serialVersionUID = 1L;
}
