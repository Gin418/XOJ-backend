package com.yupi.yuojbackendjudgeservice.judge;

import cn.hutool.json.JSONUtil;
import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.exception.BusinessException;
import com.yupi.yuojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.yupi.yuojbackendjudgeservice.judge.codesandbox.CodeSandboxFactory;
import com.yupi.yuojbackendjudgeservice.judge.codesandbox.CodeSandboxProxy;
import com.yupi.yuojbackendjudgeservice.judge.strategy.JudgeContext;
import com.yupi.yuojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.yupi.yuojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.yupi.yuojbackendmodel.model.codesandbox.JudgeInfo;
import com.yupi.yuojbackendmodel.model.dto.question.JudgeCase;
import com.yupi.yuojbackendmodel.model.dto.question.JudgeConfig;
import com.yupi.yuojbackendmodel.model.entity.Question;
import com.yupi.yuojbackendmodel.model.entity.QuestionSubmit;
import com.yupi.yuojbackendmodel.model.enums.JudgeInfoMessageEnum;
import com.yupi.yuojbackendserviceclient.service.QuestionFeignClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionFeignClient questionFeignClient;

    @Resource
    private JudgeManager judgeManager;

    @Value("${codesandbox.type:example}")
    private String type;


    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        // 1）传入题目的提交 id，获取到对应的题目、提交信息（包含代码、编程语言等）
        QuestionSubmit questionSubmit = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionFeignClient.getQuestionById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        // 2）如果题目提交状态不为等待评测，就不用重复执行了（判题状态由 status 字段承载）
        if (!JudgeInfoMessageEnum.WAITING_JUDGE.getValue().equals(questionSubmit.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在评测中");
        }
        // 3）更改判题（题目提交）的状态为 “正在测评”，防止重复执行
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(JudgeInfoMessageEnum.JUDGING.getValue());
        boolean update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
        // 4）调用沙箱，获取到执行结果
        CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
        codeSandbox = new CodeSandboxProxy(codeSandbox);
        String language = questionSubmit.getLanguage();
        String code = questionSubmit.getCode();
        // 获取输入用例（从 inOutPath 目录下读取 *.in 文件，期望输出对应同名 .out 文件）
        String inOutPath = question.getInOutPath();
        List<JudgeCase> judgeCaseList = readJudgeCasesFromPath(inOutPath, questionId);

        String judgeConfigStr = question.getJudgeConfig();
        JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(code)
                .language(language)
                .inputList(inputList)
                .timeLimit(judgeConfig.getTimeLimit())
                .memoryLimit(judgeConfig.getMemoryLimit())
                .build();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        List<String> outputList = executeCodeResponse.getOutputList();
        // 5）根据沙箱的执行结果，设置题目的判题状态和信息
        JudgeContext judgeContext = new JudgeContext();
        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
        judgeContext.setInputList(inputList);
        judgeContext.setOutputList(outputList);
        judgeContext.setJudgeCaseList(judgeCaseList);
        judgeContext.setQuestion(question);
        judgeContext.setQuestionSubmit(questionSubmit);
        judgeContext.setSandboxMessage(executeCodeResponse.getMessage());
        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);
        // 6）修改数据库中的判题结果（判题结果写入 status，judgeInfo 仅保存执行信息）
        questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        questionSubmitUpdate.setStatus(judgeContext.getStatus());
        questionSubmitUpdate.setCodeSize(executeCodeResponse.getCodeSize());
        update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
        // 判题通过时，题目通过数 +1
        if (JudgeInfoMessageEnum.ACCEPTED.getValue().equals(judgeContext.getStatus())) {
            questionFeignClient.updateQuestionAcceptNum(questionId);
        }
        QuestionSubmit questionSubmitResult = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        return questionSubmitResult;
    }

    /**
     * 从测试点目录读取所有 *.in 输入文件及对应同名 *.out 输出文件，构造成用例列表
     *
     * @param inOutPath 测试点目录路径
     */
    private List<JudgeCase> readJudgeCasesFromPath(String inOutPath, Long questionId) {
        String filePath = questionFeignClient.downloadInOutFile(inOutPath, questionId);
        File dir = new File(filePath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "测试点目录不存在: " + filePath);
        }
        File[] inFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".in"));
        if (inFiles == null || inFiles.length == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "测试点目录下不存在 .in 文件: " + filePath);
        }
        // 按文件名中的数字自然排序（如 1.in、2.in、10.in），保证用例顺序稳定
        Arrays.sort(inFiles, Comparator.comparing(JudgeServiceImpl::parseFileNo));
        List<JudgeCase> judgeCaseList = new ArrayList<>();
        for (File inFile : inFiles) {
            JudgeCase judgeCase = new JudgeCase();
            judgeCase.setInput(readFileContent(inFile));
            // 期望输出：与 .in 同名的 .out 文件
            String baseName = inFile.getName().substring(0, inFile.getName().lastIndexOf('.'));
            File outFile = new File(dir, baseName + ".out");
            if (!outFile.exists()) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "测试点缺少输出文件: " + outFile.getName());
            }
            judgeCase.setOutput(readFileContent(outFile));
            judgeCaseList.add(judgeCase);
        }
        return judgeCaseList;
    }

    /**
     * 提取文件名中的数字用于自然排序，无法解析时回退字典序
     */
    private static long parseFileNo(File file) {
        String baseName = file.getName().substring(0, file.getName().lastIndexOf('.'));
        try {
            return Long.parseLong(baseName);
        } catch (NumberFormatException e) {
            return baseName.hashCode();
        }
    }

    /**
     * 读取测试点文件内容（UTF-8）
     */
    private String readFileContent(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取测试点文件失败: " + file.getName());
        }
    }
}
