package com.yupi.yuojbackendserviceclient.service;


import com.yupi.yuojbackendcommon.common.BaseResponse;
import com.yupi.yuojbackendcommon.common.ResultUtils;
import com.yupi.yuojbackendmodel.model.dto.competition.CompetitionSubmitInnerQueryRequest;
import com.yupi.yuojbackendmodel.model.entity.Question;
import com.yupi.yuojbackendmodel.model.entity.QuestionSubmit;
import com.yupi.yuojbackendmodel.model.vo.InnerQuestionSubmitPageVO;
import com.yupi.yuojbackendmodel.model.vo.QuestionSimpleVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
* @author 李鱼皮
* @description 针对表【question(题目)】的数据库操作Service
* @createDate 2023-08-07 20:58:00
*/
@FeignClient(name = "yuoj-backend-question-service", path = "/api/question/inner")
public interface QuestionFeignClient {

    @GetMapping("/get/id")
    Question getQuestionById(@RequestParam("questionId") long questionId);

    /**
     * 按题目 id 列表批量查询题目简信息（题目 id、题号、标题，按传入顺序返回）
     */
    @PostMapping("/question/simple/list")
    List<QuestionSimpleVO> listQuestionSimpleByIds(@RequestBody List<Long> questionIds);

    @GetMapping("/question_submit/get/id")
    QuestionSubmit getQuestionSubmitById(@RequestParam("questionId") long questionSubmitId);

    @PostMapping("/question_submit/update")
    boolean updateQuestionSubmitById(@RequestBody QuestionSubmit questionSubmit);

    @PostMapping("/question/update/submit_num")
    boolean updateQuestionSubmitNum(@RequestParam("questionId") long questionId);

    @PostMapping("/question/update/accept_num")
    boolean updateQuestionAcceptNum(@RequestParam("questionId") long questionId);

    @GetMapping("/downloadInOutFile")
    String downloadInOutFile(@RequestParam("rawUrl") String rawUrl, @RequestParam("questionId") Long questionId);

    /**
     * 分页查询竞赛提交记录
     */
    @PostMapping("/question_submit/competition/page")
    InnerQuestionSubmitPageVO listCompetitionSubmits(@RequestBody CompetitionSubmitInnerQueryRequest request);

    /**
     * 查询竞赛的全部提交记录（按提交时间升序）
     */
    @GetMapping("/question_submit/competition/list")
    List<QuestionSubmit> listCompetitionSubmitsAll(@RequestParam("competitionId") long competitionId);

    /**
     * 查询竞赛中指定用户列表的全部提交记录（按提交时间升序）
     */
    @PostMapping("/question_submit/competition/list/byUserIds")
    List<QuestionSubmit> listCompetitionSubmitsByUserIds(@RequestBody CompetitionSubmitInnerQueryRequest request);

}
