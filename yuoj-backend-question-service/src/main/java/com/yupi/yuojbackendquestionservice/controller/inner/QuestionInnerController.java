package com.yupi.yuojbackendquestionservice.controller.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yuojbackendcommon.common.BaseResponse;
import com.yupi.yuojbackendcommon.common.ResultUtils;
import com.yupi.yuojbackendcommon.constant.CommonConstant;
import com.yupi.yuojbackendcommon.utils.SqlUtils;
import com.yupi.yuojbackendmodel.model.dto.competition.CompetitionSubmitInnerQueryRequest;
import com.yupi.yuojbackendmodel.model.entity.Question;
import com.yupi.yuojbackendmodel.model.entity.QuestionSubmit;
import com.yupi.yuojbackendmodel.model.vo.InnerQuestionSubmitPageVO;
import com.yupi.yuojbackendquestionservice.service.FileService;
import com.yupi.yuojbackendquestionservice.service.QuestionService;
import com.yupi.yuojbackendquestionservice.service.QuestionSubmitService;
import com.yupi.yuojbackendserviceclient.service.QuestionFeignClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

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

    /**
     * 分页查询竞赛提交记录
     */
    @PostMapping("/question_submit/competition/page")
    @Override
    public InnerQuestionSubmitPageVO listCompetitionSubmits(@RequestBody CompetitionSubmitInnerQueryRequest request) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("competitionId", request.getCompetitionId());
        queryWrapper.eq(ObjectUtils.isNotEmpty(request.getQuestionId()), "questionId", request.getQuestionId());
        queryWrapper.eq(StringUtils.isNotBlank(request.getLanguage()), "language", request.getLanguage());
        queryWrapper.eq(StringUtils.isNotBlank(request.getStatus()), "status", request.getStatus());
        queryWrapper.in(CollectionUtils.isNotEmpty(request.getUserIds()), "userId", request.getUserIds());
        queryWrapper.eq("isDelete", false);
        // 排序：默认按提交时间倒序，支持指定字段
        if (SqlUtils.validSortField(request.getSortField())) {
            queryWrapper.orderBy(true, CommonConstant.SORT_ORDER_ASC.equals(request.getSortOrder()), request.getSortField());
        } else {
            queryWrapper.orderByDesc("createTime");
        }
        Page<QuestionSubmit> page = questionSubmitService.page(new Page<>(request.getCurrent(), request.getPageSize()), queryWrapper);
        InnerQuestionSubmitPageVO result = new InnerQuestionSubmitPageVO();
        result.setTotal(page.getTotal());
        result.setRecords(page.getRecords());
        return result;
    }

    /**
     * 查询竞赛的全部提交记录（按提交时间升序，仅查排名所需字段）
     */
    @GetMapping("/question_submit/competition/list")
    @Override
    public List<QuestionSubmit> listCompetitionSubmitsAll(@RequestParam("competitionId") long competitionId) {
        return questionSubmitService.lambdaQuery()
                .eq(QuestionSubmit::getCompetitionId, competitionId)
                .select(QuestionSubmit::getId, QuestionSubmit::getQuestionId, QuestionSubmit::getUserId,
                        QuestionSubmit::getStatus, QuestionSubmit::getCreateTime)
                .orderByAsc(QuestionSubmit::getCreateTime)
                .list();
    }

}
