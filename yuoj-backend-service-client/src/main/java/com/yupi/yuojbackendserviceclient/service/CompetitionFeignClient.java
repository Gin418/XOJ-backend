package com.yupi.yuojbackendserviceclient.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 竞赛服务内部接口（仅服务间调用，不对外）
 */
@FeignClient(name = "xoj-backend-competition-service", path = "/api/competition/inner")
public interface CompetitionFeignClient {

    /**
     * 竞赛题目提交数 +1
     */
    @PostMapping("/competition_question/update/submit_num")
    boolean updateCompetitionQuestionSubmitNum(@RequestParam("competitionId") long competitionId,
                                               @RequestParam("questionId") long questionId);

    /**
     * 竞赛题目通过数 +1，并按提交时间记录第一个通过的用户（首杀）
     *
     * @param acceptTime 通过用户的提交时间（yyyy-MM-dd HH:mm:ss）
     */
    @PostMapping("/competition_question/update/accept_num")
    boolean updateCompetitionQuestionAcceptNum(@RequestParam("competitionId") long competitionId,
                                               @RequestParam("questionId") long questionId,
                                               @RequestParam("userId") long userId,
                                               @RequestParam("acceptTime") String acceptTime);
}
