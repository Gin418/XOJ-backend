package com.yupi.yuojbackendmodel.model.codesandbox;

import com.yupi.yuojbackendmodel.model.dto.question.JudgeConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeRequest {

    private List<String> inputList;

    private String code;

    private String language;

    private Long timeLimit;

    private Long memoryLimit;
}
