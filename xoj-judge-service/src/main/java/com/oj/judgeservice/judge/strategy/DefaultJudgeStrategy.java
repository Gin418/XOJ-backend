package com.oj.judgeservice.judge.strategy;

import cn.hutool.json.JSONUtil;
import com.oj.model.codesandbox.JudgeInfo;
import com.oj.model.dto.question.JudgeCase;
import com.oj.model.dto.question.JudgeConfig;
import com.oj.model.entity.Question;
import com.oj.model.enums.JudgeInfoMessageEnum;

import java.util.List;
import java.util.Optional;

/**
 * 默认判题策略
 */
public class DefaultJudgeStrategy implements JudgeStrategy {

    /**
     * 执行判题
     * @param judgeContext
     * @return
     */
    @Override
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        JudgeInfo judgeInfo = judgeContext.getJudgeInfo();
        Long memory = Optional.ofNullable(judgeInfo.getMemory()).orElse(0L);
        Long time = Optional.ofNullable(judgeInfo.getTime()).orElse(0L);
        List<String> inputList = judgeContext.getInputList();
        List<String> outputList = judgeContext.getOutputList();
        Question question = judgeContext.getQuestion();
        List<JudgeCase> judgeCaseList = judgeContext.getJudgeCaseList();
        JudgeInfoMessageEnum judgeInfoMessageEnum = JudgeInfoMessageEnum.ACCEPTED;
        JudgeInfo judgeInfoResponse = new JudgeInfo();
        String sandboxMessage = judgeInfo.getMessage();
        // 沙箱端已给出判定结果（编译错误等）时直接采用
        if (JudgeInfoMessageEnum.COMPILE_ERROR.getValue().equals(sandboxMessage)) {
            judgeContext.setStatus(JudgeInfoMessageEnum.COMPILE_ERROR.getValue());
            return judgeInfoResponse;
        }
        judgeInfoResponse.setMemory(memory);
        judgeInfoResponse.setTime(time);
        // 先判断题目限制
        String judgeConfigStr = question.getJudgeConfig();
        JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);
        Long needMemoryLimit = judgeConfig.getMemoryLimit();
        Long needTimeLimit = judgeConfig.getTimeLimit();
        // 内存限制单位为 MB，沙箱返回的 memory 单位为 KB，需转换为 KB 再比较
        if (JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED.getValue().equals(sandboxMessage)) {
            judgeContext.setStatus(JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED.getValue());
            return judgeInfoResponse;
        }
        // Java 程序需要时间为其他语言的10倍
//        long JAVA_PROGRAM_TIME_COST = 10000L;
        if (JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getValue().equals(sandboxMessage)) {
            judgeContext.setStatus(JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getValue());
            return judgeInfoResponse;
        }
        // 再判断沙箱执行的结果输出数量是否和预期输出数量相等
        if (outputList.size() != inputList.size()) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
            judgeContext.setStatus(judgeInfoMessageEnum.getValue());
            return judgeInfoResponse;
        }
        // 依次判断每一项输出和预期输出是否相等（忽略输出结尾的换行符）
        for (int i = 0; i < judgeCaseList.size(); i++) {
            JudgeCase judgeCase = judgeCaseList.get(i);
            String expected = normalizeOutput(judgeCase.getOutput());
            String actual = normalizeOutput(outputList.get(i));
            if (expected.equals(actual)) continue;

            if (expected.replaceAll("\\s+", " ").trim()
                    .equals(actual.replaceAll("\\s+", " ").trim())) {
                judgeInfoMessageEnum = JudgeInfoMessageEnum.PRESENTATION_ERROR;
                judgeContext.setStatus(judgeInfoMessageEnum.getValue());
                return judgeInfoResponse;
            }

            judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
            judgeContext.setStatus(judgeInfoMessageEnum.getValue());
            return judgeInfoResponse;
        }
        judgeContext.setStatus(judgeInfoMessageEnum.getValue());
        return judgeInfoResponse;
    }

    /**
     * 归一化输出：将换行符统一为 \n，并去掉字符串结尾的换行符（\n、\r）
     *
     * @param str
     * @return
     */
    private String normalizeOutput(String str) {
        if (str == null) {
            return null;
        }
        // 统一换行符：\r\n -> \n
        String normalized = str.replace("\r\n", "\n");
        int end = normalized.length();
        while (end > 0 && (normalized.charAt(end - 1) == '\n' || normalized.charAt(end - 1) == '\r')) {
            end--;
        }
        return normalized.substring(0, end);
    }
}
