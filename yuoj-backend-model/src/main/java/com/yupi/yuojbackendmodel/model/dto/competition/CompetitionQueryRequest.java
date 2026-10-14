package com.yupi.yuojbackendmodel.model.dto.competition;

import com.yupi.yuojbackendcommon.common.PageRequest;
import lombok.Data;

@Data
public class CompetitionQueryRequest extends PageRequest {

    private String competitionTitle;

    private String status;
}
