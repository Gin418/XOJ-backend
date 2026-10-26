package com.oj.judgeservice.judge.strategy;

import com.oj.model.codesandbox.JudgeInfo;
import com.oj.model.dto.question.JudgeCase;
import com.oj.model.entity.Question;
import com.oj.model.entity.QuestionSubmit;
import lombok.Data;

import java.util.List;

/**
 * 上下文（用于定义在策略中传递的参数）
 */
@Data
public class JudgeContext {

    private JudgeInfo judgeInfo;

    private List<String> inputList;

    private List<String> outputList;

    private List<JudgeCase> judgeCaseList;

    private Question question;

    private QuestionSubmit questionSubmit;

    private Long codeSize;

    /**
     * 沙箱接口返回的执行信息（用于判断沙箱端判定结果，如编译错误、内存溢出等）
     */
    private String sandboxMessage;

    /**
     * 最终判题状态（由判题策略设置，取值 JudgeInfoMessageEnum）
     */
    private String status;

}
