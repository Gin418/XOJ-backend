package com.yupi.yuojbackendquestionservice.controller.inner;

import com.yupi.yuojbackendcommon.common.BaseResponse;
import com.yupi.yuojbackendcommon.common.ResultUtils;
import com.yupi.yuojbackendmodel.model.entity.Question;
import com.yupi.yuojbackendmodel.model.entity.QuestionSubmit;
import com.yupi.yuojbackendquestionservice.service.FileService;
import com.yupi.yuojbackendquestionservice.service.QuestionService;
import com.yupi.yuojbackendquestionservice.service.QuestionSubmitService;
import com.yupi.yuojbackendserviceclient.service.QuestionFeignClient;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 该服务仅内部调用，不是给前端的
 */
@RestController
@RequestMapping("/inner")
public class QuestionInnerController implements QuestionFeignClient {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private FileService fileService;

    @GetMapping("/get/id")
    @Override
    public Question getQuestionById(@RequestParam("questionId") long questionId) {
        return questionService.getById(questionId);
    }

    @GetMapping("/question_submit/get/id")
    @Override
    public QuestionSubmit getQuestionSubmitById(@RequestParam("questionId") long questionSubmitId) {
        return questionSubmitService.getById(questionSubmitId);
    }

    @PostMapping("/question_submit/update")
    @Override
    public boolean updateQuestionSubmitById(@RequestBody QuestionSubmit questionSubmit) {
        return questionSubmitService.updateById(questionSubmit);
    }

    @PostMapping("/question/update/submit_num")
    @Override
    public boolean updateQuestionSubmitNum(@RequestParam("questionId") long questionId) {
        return questionService.updateQuestionSubmitNum(questionId);
    }

    @PostMapping("/question/update/accept_num")
    @Override
    public boolean updateQuestionAcceptNum(@RequestParam("questionId") long questionId) {
        return questionService.updateQuestionAcceptNum(questionId);
    }

    @Override
    @GetMapping("/downloadInOutFile")
    public String downloadInOutFile(@RequestParam("rawUrl") String rawUrl, @RequestParam("questionId") Long questionId) {
        return fileService.downloadFile(rawUrl, questionId);
    }

}
