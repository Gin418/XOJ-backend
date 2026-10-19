package com.xoj.xojbackendcompetitionservice.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xoj.xojbackendcompetitionservice.service.CompetitionQuestionService;
import com.yupi.yuojbackendcommon.common.BaseResponse;
import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.common.ResultUtils;
import com.yupi.yuojbackendcommon.exception.BusinessException;
import com.yupi.yuojbackendcommon.exception.ThrowUtils;
import com.yupi.yuojbackendcommon.utils.GatewayHeaderUtil;
import com.yupi.yuojbackendmodel.model.codesandbox.JudgeInfo;
import com.yupi.yuojbackendmodel.model.dto.competition.CompetitionSubmitInnerQueryRequest;
import com.yupi.yuojbackendmodel.model.dto.competition.CompetitionSubmitQueryRequest;
import com.yupi.yuojbackendmodel.model.entity.CompetitionQuestion;
import com.yupi.yuojbackendmodel.model.entity.QuestionSubmit;
import com.yupi.yuojbackendmodel.model.entity.User;
import com.yupi.yuojbackendmodel.model.vo.CompetitionSubmitVO;
import com.yupi.yuojbackendmodel.model.vo.InnerQuestionSubmitPageVO;
import com.yupi.yuojbackendmodel.model.vo.QuestionSimpleVO;
import com.yupi.yuojbackendserviceclient.service.QuestionFeignClient;
import com.yupi.yuojbackendserviceclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 竞赛提交记录接口
 */
@RestController
@RequestMapping("/submit")
@Slf4j
public class CompetitionSubmitController {

    @Resource
    private CompetitionQuestionService competitionQuestionService;

    @Resource
    private QuestionFeignClient questionFeignClient;

    @Resource
    private UserFeignClient userFeignClient;

    /**
     * 分页查询竞赛的提交记录，搜索条件：题号、语言、测评状态、用户昵称、是否只看自己的提交
     *
     * @param request 查询请求
     */
    @PostMapping("/page")
    public BaseResponse<Page<CompetitionSubmitVO>> listCompetitionSubmits(@RequestBody CompetitionSubmitQueryRequest request) {
        if (request == null || request.getCompetitionId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        log.info("listCompetitionSubmits request: {}", request);
        Long competitionId = request.getCompetitionId();
        long current = request.getCurrent();
        long pageSize = request.getPageSize();
        Long questionId = request.getQuestionId();

        // 1）是否只看自己的提交：解析当前登录用户
        Long loginUserId = GatewayHeaderUtil.getUserId();
        Long queryUserId = null;
        if (Boolean.TRUE.equals(request.getOnlyMine())) {
            ThrowUtils.throwIf(loginUserId == null || loginUserId <= 0, ErrorCode.NOT_LOGIN_ERROR);
            queryUserId = loginUserId;
        }

        // 3）用户昵称 → 用户 id 列表
        List<Long> userIds = null;
        if (StringUtils.isNotBlank(request.getUserName())) {
            List<User> users = userFeignClient.listByUserName(request.getUserName());
            if (CollectionUtils.isEmpty(users)) {
                return ResultUtils.success(new Page<>(current, pageSize, 0));
            }
            userIds = users.stream().map(User::getId).collect(Collectors.toList());
        }
        // 只看自己的提交：过滤为当前登录用户
        if (queryUserId != null) {
            if (userIds != null && !userIds.contains(queryUserId)) {
                return ResultUtils.success(new Page<>(current, pageSize, 0));
            }
            userIds = Collections.singletonList(queryUserId);
        }

        // 4）调用题目服务分页查询提交记录
        CompetitionSubmitInnerQueryRequest innerRequest = new CompetitionSubmitInnerQueryRequest();
        innerRequest.setCompetitionId(competitionId);
        innerRequest.setQuestionId(questionId);
        innerRequest.setLanguage(request.getLanguage());
        innerRequest.setStatus(request.getStatus());
        innerRequest.setUserIds(userIds);
        innerRequest.setCurrent(current);
        innerRequest.setPageSize(pageSize);
        innerRequest.setSortField(request.getSortField());
        innerRequest.setSortOrder(request.getSortOrder());
        InnerQuestionSubmitPageVO innerPage = questionFeignClient.listCompetitionSubmits(innerRequest);
        List<QuestionSubmit> submitList = innerPage.getRecords();
        if (CollectionUtils.isEmpty(submitList)) {
            return ResultUtils.success(new Page<>(current, pageSize, 0));
        }

        // 5）组装 VO：题号、题目标题、用户信息
        Map<Long, Integer> questionIdOrderMap = competitionQuestionService.listByCompetitionId(competitionId).stream()
                .collect(Collectors.toMap(CompetitionQuestion::getQuestionId, CompetitionQuestion::getQuestionOrder, (o1, o2) -> o1));
        Set<Long> questionIdSet = submitList.stream().map(QuestionSubmit::getQuestionId).collect(Collectors.toSet());
        Map<Long, String> questionTitleMap = questionFeignClient.listQuestionSimpleByIds(new ArrayList<>(questionIdSet)).stream()
                .collect(Collectors.toMap(QuestionSimpleVO::getId, QuestionSimpleVO::getTitle, (t1, t2) -> t1));
        Map<Long, User> userMap = new HashMap<>();
        Set<Long> userIdSet = submitList.stream().map(QuestionSubmit::getUserId).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(userIdSet)) {
            userMap = userFeignClient.listByIds(userIdSet).stream()
                    .collect(Collectors.toMap(User::getId, user -> user, (u1, u2) -> u1));
        }

        Map<Long, User> finalUserMap = userMap;
        List<CompetitionSubmitVO> submitVOList = submitList.stream().map(submit -> {
            CompetitionSubmitVO submitVO = new CompetitionSubmitVO();
            submitVO.setId(submit.getId());
            submitVO.setQuestionId(submit.getQuestionId());
            submitVO.setCustomId(competitionQuestionService.convertToQuestionNo(questionIdOrderMap.get(submit.getQuestionId())));
            submitVO.setQuestionTitle(questionTitleMap.get(submit.getQuestionId()));
            submitVO.setLanguage(submit.getLanguage());
            submitVO.setStatus(submit.getStatus());
            submitVO.setJudgeInfo(JSONUtil.toBean(submit.getJudgeInfo(), JudgeInfo.class));
            submitVO.setCodeSize(submit.getCodeSize());
            submitVO.setUserId(submit.getUserId());
            // 仅返回自己提交的代码，他人提交的代码置空
            if (loginUserId != null && loginUserId > 0 && loginUserId.equals(submit.getUserId())) {
                submitVO.setCode(submit.getCode());
            }
            submitVO.setUserVO(userFeignClient.getUserVO(finalUserMap.get(submit.getUserId())));
            submitVO.setCreateTime(submit.getCreateTime());
            return submitVO;
        }).collect(Collectors.toList());

        Page<CompetitionSubmitVO> resultPage = new Page<>(current, pageSize, innerPage.getTotal());
        resultPage.setRecords(submitVOList);
        return ResultUtils.success(resultPage);
    }
}
