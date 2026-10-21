package com.yupi.yuojbackendmodel.model.vo;

import cn.hutool.core.convert.Convert;
import com.yupi.yuojbackendmodel.model.dto.question.JudgeCase;
import com.yupi.yuojbackendmodel.model.dto.question.JudgeConfig;
import com.yupi.yuojbackendmodel.model.entity.Competition;
import com.yupi.yuojbackendmodel.model.entity.Question;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.List;

@Data
public class CompetitionVO {

    private Long id;

    private String competitionTitle;

    private String description;

    private String startTime;

    private String endTime;

    /**
     * 封榜时间（封榜后排名不再更新）
     */
    private String freezeTime;

    /**
     * 是否允许赛后提交（0 不允许、1 允许）
     */
    private Integer allowSubmitAfterEnd;

    /**
     * 是否揭榜（0 未揭榜、1 已揭榜）
     */
    private Integer isRevealed;

    private String status;

    public static Competition voToObj(CompetitionVO competitionVO) {
        if (competitionVO == null) {
            return null;
        }
        Competition competition = new Competition();
        BeanUtils.copyProperties(competitionVO, competition);

        return competition;
    }

    public static CompetitionVO objToVo(Competition competition) {
        if (competition == null) {
            return null;
        }
        CompetitionVO competitionVO = new CompetitionVO();
        BeanUtils.copyProperties(competition, competitionVO);

        return competitionVO;
    }
}
