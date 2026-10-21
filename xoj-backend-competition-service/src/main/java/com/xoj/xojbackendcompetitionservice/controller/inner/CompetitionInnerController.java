package com.xoj.xojbackendcompetitionservice.controller.inner;

import com.xoj.xojbackendcompetitionservice.service.CompetitionQuestionService;
import com.xoj.xojbackendcompetitionservice.service.CompetitionService;
import com.yupi.yuojbackendmodel.model.entity.Competition;
import com.yupi.yuojbackendserviceclient.service.CompetitionFeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 该服务仅内部调用，不是给前端的
 */
@RestController
@RequestMapping("/inner")
public class CompetitionInnerController implements CompetitionFeignClient {

    @Resource
    private CompetitionQuestionService competitionQuestionService;

    @Resource
    private CompetitionService competitionService;

    @PostMapping("/get")
    @Override
    public Competition getCompetitionById(@RequestParam("competitionId") long competitionId) {
        return competitionService.getById(competitionId);
    }

    @PostMapping("/competition_question/update/submit_num")
    @Override
    public boolean updateCompetitionQuestionSubmitNum(@RequestParam("competitionId") long competitionId,
                                                      @RequestParam("questionId") long questionId) {
        return competitionQuestionService.updateSubmitNum(competitionId, questionId);
    }

    @PostMapping("/competition_question/update/accept_num")
    @Override
    public boolean updateCompetitionQuestionAcceptNum(@RequestParam("competitionId") long competitionId,
                                                      @RequestParam("questionId") long questionId,
                                                      @RequestParam("userId") long userId,
                                                      @RequestParam("acceptTime") String acceptTime) {
        return competitionQuestionService.updateAcceptNum(competitionId, questionId, userId, acceptTime);
    }
}
