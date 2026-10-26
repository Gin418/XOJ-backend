package com.oj.questionservice.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.oj.common.annotation.AuthCheck;
import com.oj.common.common.BaseResponse;
import com.oj.common.common.DeleteRequest;
import com.oj.common.common.ErrorCode;
import com.oj.common.common.ResultUtils;
import com.oj.common.constant.GatewayConsts;
import com.oj.common.constant.UserConstant;
import com.oj.common.exception.BusinessException;
import com.oj.common.exception.ThrowUtils;
import com.oj.common.utils.GatewayHeaderUtil;
import com.oj.model.dto.question.*;
import com.oj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.oj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.oj.model.entity.Question;
import com.oj.model.entity.QuestionSubmit;
import com.oj.model.entity.User;
import com.oj.model.enums.EnumVo;
import com.oj.model.enums.QuestionSubmitLanguageEnum;
import com.oj.model.vo.DailyQuestionRankItemVO;
import com.oj.model.vo.DailySubmitStatsVO;
import com.oj.model.vo.MySubmitStatsVO;
import com.oj.model.vo.QuestionSimpleVO;
import com.oj.model.vo.QuestionSubmitCountVo;
import com.oj.model.vo.QuestionSubmitVO;
import com.oj.model.vo.QuestionVO;
import com.oj.questionservice.service.FileService;
import com.oj.questionservice.service.QuestionService;
import com.oj.questionservice.service.QuestionSubmitService;
import com.oj.serviceclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目接口
 */
@RestController
@RequestMapping("/")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private FileService fileService;

    @Resource
    private UserFeignClient userFeignClient;

    private final static Gson GSON = new Gson();

    /**
     * 最近提交统计的最大天数
     */
    private static final int MAX_RECENT_STATS_DAYS = 365;

    // region 增删改查

    /**
     * 创建
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        if (questionAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<String> tags = questionAddRequest.getTags();
        if (tags != null) {
            question.setTags(GSON.toJson(tags));
        }
        List<JudgeCase> judgeCase = questionAddRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(GSON.toJson(judgeCase));
        }
        JudgeConfig judgeConfig = questionAddRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(GSON.toJson(judgeConfig));
        }
        // 保存测试点路径
        question.setInOutPath(questionAddRequest.getInOutPath());
        questionService.validQuestion(question, true);
        question.setUserId(GatewayHeaderUtil.getUserId());
        question.setFavourNum(0);
        question.setThumbNum(0);
        question.setSubmitNum(0);
        question.setAcceptedNum(0);
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(GatewayHeaderUtil.getUserId())
                && !UserConstant.ADMIN_ROLE.equals(GatewayHeaderUtil.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = questionService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        List<String> tags = questionUpdateRequest.getTags();
        if (tags != null) {
            question.setTags(GSON.toJson(tags));
        }
        List<JudgeCase> judgeCase = questionUpdateRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(GSON.toJson(judgeCase));
        }
        JudgeConfig judgeConfig = questionUpdateRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(GSON.toJson(judgeConfig));
        }
        // 保存测试点路径
        question.setInOutPath(questionUpdateRequest.getInOutPath());
        // 参数校验
        questionService.validQuestion(question, false);
        long id = questionUpdateRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = questionService.updateById(question);
        // 题目更新成功后，删除旧的测试点下载缓存，避免残留过期文件
        if (result) {
            fileService.deleteQuestionFiles(id);
        }
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Question> getQuestionById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 不是本人或管理员，不能直接获取所有信息
        if (!question.getUserId().equals(GatewayHeaderUtil.getUserId())
                && !UserConstant.ADMIN_ROLE.equals(GatewayHeaderUtil.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return ResultUtils.success(question);
    }

    /**
     * 根据 id 获取（脱敏）
     *
     * @param customId 题目自定义 id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(String customId, HttpServletRequest request) {
        if (StringUtils.isBlank(customId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("customId", customId);
        Question question = questionService.getOne(queryWrapper);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(questionService.getQuestionVO(question, request));
    }

    /**
     * 获取每日一题（以当天日期为随机种子，同一天内题目固定，次日自动更换）
     *
     * @param request
     * @return
     */
    @GetMapping("/daily")
    public BaseResponse<QuestionVO> getDailyQuestion(HttpServletRequest request) {
        Question dailyQuestion = questionService.getDailyQuestion();
        return ResultUtils.success(questionService.getQuestionVO(dailyQuestion, request));
    }

    /**
     * 获取每日一题解题榜（当天在每日一题上首次通过时间最早的前 N 名）
     *
     * @param limit 榜单人数，默认 10，最大 10
     * @return
     */
    @GetMapping("/daily/rank")
    public BaseResponse<List<DailyQuestionRankItemVO>> getDailyQuestionRank(@RequestParam(defaultValue = "10") int limit) {
        ThrowUtils.throwIf(limit < 1 || limit > 10, ErrorCode.PARAMS_ERROR, "榜单人数需在 1~10 之间");
        return ResultUtils.success(questionSubmitService.getDailyQuestionRank(limit));
    }

    /**
     * 按题目 id 列表查询题目简信息（题目 id、题号、标题）
     *
     * @param questionIds 题目 id 列表
     * @return
     */
    @PostMapping("/simple/list")
    public BaseResponse<List<QuestionSimpleVO>> listSimpleQuestions(@RequestBody List<Long> questionIds) {
        if (CollectionUtils.isEmpty(questionIds)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(questionService.listSimpleByIds(questionIds));
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
            HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
            HttpServletRequest request) {
        if (questionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        questionQueryRequest.setUserId(GatewayHeaderUtil.getUserId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 分页获取题目列表（仅管理员）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                   HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionPage);
    }

    // endregion

    /**
     * 编辑（用户）
     *
     * @param questionEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        List<String> tags = questionEditRequest.getTags();
        if (tags != null) {
            question.setTags(GSON.toJson(tags));
        }
        List<JudgeCase> judgeCase = questionEditRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(GSON.toJson(judgeCase));
        }
        JudgeConfig judgeConfig = questionEditRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(GSON.toJson(judgeConfig));
        }
        // 保存测试点路径
        question.setInOutPath(questionEditRequest.getInOutPath());
        // 参数校验
        questionService.validQuestion(question, false);
        long id = questionEditRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(GatewayHeaderUtil.getUserId())
                && !UserConstant.ADMIN_ROLE.equals(GatewayHeaderUtil.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = questionService.updateById(question);
        return ResultUtils.success(result);
    }

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param request
     * @return 提交记录的 id
     */
    @PostMapping("/question_submit/do")
    public BaseResponse<Long> doQuestionSubmit(@RequestBody QuestionSubmitAddRequest questionSubmitAddRequest,
                                               HttpServletRequest request) {
        if (questionSubmitAddRequest == null || questionSubmitAddRequest.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能提交
        long questionSubmitId = questionSubmitService.doQuestionSubmit(questionSubmitAddRequest,
                GatewayHeaderUtil.getUserId());
        return ResultUtils.success(questionSubmitId);
    }

    /**
     * 分页获取题目提交列表（除了管理员外，普通用户只能看到非答案、提交代码等公开信息）
     *
     * @param questionSubmitQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/question_submit/list/page")
    public BaseResponse<Page<QuestionSubmitVO>> listQuestionSubmitByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest,
                                                                         HttpServletRequest request) {
        long current = questionSubmitQueryRequest.getCurrent();
        long size = questionSubmitQueryRequest.getPageSize();
        // 只查自己的提交时，必须已登录；未登录时该条件不生效，按公开列表正常查询
        if (questionSubmitQueryRequest.getOnlyMine()) {
            Long loginUserId = GatewayHeaderUtil.getUserId();
            if (loginUserId > 0) {
                questionSubmitQueryRequest.setUserId(loginUserId);
            } else {
                // 未登录：onlyMine 条件不生效，正常查询公开提交列表
                questionSubmitQueryRequest.setOnlyMine(false);
            }
        }
        // 按用户昵称模糊查询，先转为用户 id 列表
        String userName = questionSubmitQueryRequest.getUserName();
        if (StringUtils.isNotBlank(userName)) {
            List<Long> userIds = userFeignClient.listByUserName(userName).stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            // 未匹配到任何用户，直接返回空页
            if (CollectionUtils.isEmpty(userIds)) {
                return ResultUtils.success(new Page<>());
            }
            questionSubmitQueryRequest.setUserIds(userIds);
        }
        QuestionQueryRequest questionQueryRequest = new QuestionQueryRequest();
        questionQueryRequest.setCustomId(questionSubmitQueryRequest.getCustomId());
        List<Question> questions = questionService.list(questionService.getQueryWrapper(questionQueryRequest));
        List<Long> questionIds = questions.stream().map(Question::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(questions)) {
            return ResultUtils.success(new Page<>());
        }
        questionSubmitQueryRequest.setQuestionIds(questionIds);
        // 从数据库中查询原始的题目提交分页信息
        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(new Page<>(current, size),
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        // 返回脱敏信息
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVOPage(questionSubmitPage, questions));
    }

    @GetMapping("/question_submit/my/stats")
    public BaseResponse<MySubmitStatsVO> getMySubmitStats(HttpServletRequest request) {
        return ResultUtils.success(questionSubmitService.getMySubmitStats(GatewayHeaderUtil.getUserId()));
    }

    @GetMapping("/question_submit/recent/stats")
    public BaseResponse<List<DailySubmitStatsVO>> getRecentDailySubmitStats(@RequestParam(defaultValue = "7") int days,
                                                                            @RequestParam(required = false) Long userId) {
        // 统计天数限制在 1 ~ 365 天
        ThrowUtils.throwIf(days < 1 || days > MAX_RECENT_STATS_DAYS, ErrorCode.PARAMS_ERROR,
                "统计天数需在 1~" + MAX_RECENT_STATS_DAYS + " 之间");
        // 未指定用户时统计当前登录用户，未登录抛异常；指定用户时校验参数合法性
        if (userId == null) {
            userId = GatewayHeaderUtil.getUserId();
            ThrowUtils.throwIf(userId <= 0, ErrorCode.NOT_LOGIN_ERROR);
        } else {
            ThrowUtils.throwIf(userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");
        }
        return ResultUtils.success(questionSubmitService.getRecentDailySubmitStats(userId, days));
    }

    @GetMapping("/count/{id}")
    public BaseResponse<QuestionSubmitCountVo> getQuestionSubmitCount(@PathVariable("id") long id) {
        return ResultUtils.success(questionSubmitService.getQuestionSubmitCount(id));
    }

    @GetMapping("/stats/{id}")
    public BaseResponse<QuestionSubmitCountVo> getQuestionSubmitStats(@PathVariable("id") long id) {
        return ResultUtils.success(questionSubmitService.getQuestionSubmitStats(id));
    }

    @GetMapping("/language/select")
    public BaseResponse<List<EnumVo>> languageSelect() {
        List<QuestionSubmitLanguageEnum> enumList = QuestionSubmitLanguageEnum.getValues();
        List<EnumVo> enumVoList = new ArrayList<>();
        for (QuestionSubmitLanguageEnum enumItem : enumList) {
            EnumVo enumVo = new EnumVo();
            enumVo.setType(enumItem.getLanguageId());
            enumVo.setValue(enumItem.getLanguage());
            enumVo.setText(enumItem.getLanguageTitle());
            enumVoList.add(enumVo);
        }
        return ResultUtils.success(enumVoList);
    }

}
