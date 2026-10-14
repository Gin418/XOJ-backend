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
