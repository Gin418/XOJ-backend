package com.oj.model.dto.competition;

import com.oj.common.common.PageRequest;
import lombok.Data;

@Data
public class CompetitionQueryRequest extends PageRequest {

    private String competitionTitle;

    private String status;
}
