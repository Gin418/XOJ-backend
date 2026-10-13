package com.yupi.yuojbackendquestionservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.constant.CommonConstant;
import com.yupi.yuojbackendcommon.exception.BusinessException;
import com.yupi.yuojbackendcommon.utils.GatewayHeaderUtil;
import com.yupi.yuojbackendcommon.utils.SqlUtils;
import com.yupi.yuojbackendmodel.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.yupi.yuojbackendmodel.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.yupi.yuojbackendmodel.model.entity.Question;
import com.yupi.yuojbackendmodel.model.entity.QuestionSubmit;
import com.yupi.yuojbackendmodel.model.entity.User;
import com.yupi.yuojbackendmodel.model.enums.JudgeInfoMessageEnum;
import com.yupi.yuojbackendmodel.model.enums.QuestionStatusEnum;
import com.yupi.yuojbackendmodel.model.enums.QuestionSubmitLanguageEnum;
import com.yupi.yuojbackendmodel.model.enums.UserRoleEnum;
import com.yupi.yuojbackendmodel.model.vo.MySubmitStatsVO;
import com.yupi.yuojbackendmodel.model.vo.QuestionSubmitCountVo;
import com.yupi.yuojbackendmodel.model.vo.QuestionSubmitVO;
import com.yupi.yuojbackendmodel.model.vo.QuestionVO;
import com.yupi.yuojbackendquestionservice.mapper.QuestionSubmitMapper;
import com.yupi.yuojbackendquestionservice.rabbitmq.MyMessageProducer;
import com.yupi.yuojbackendquestionservice.service.QuestionService;
import com.yupi.yuojbackendquestionservice.service.QuestionSubmitService;
import com.yupi.yuojbackendserviceclient.service.JudgeFeignClient;
import com.yupi.yuojbackendserviceclient.service.UserFeignClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
* @author 李鱼皮
* @description 针对表【question_submit(题目提交)】的数据库操作Service实现
* @createDate 2023-08-07 20:58:53
*/
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
    implements QuestionSubmitService {
    
    @Resource
    private QuestionService questionService;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    @Lazy
    private JudgeFeignClient judgeFeignClient;

    @Resource
    private MyMessageProducer myMessageProducer;

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param userId
     * @return
     */
    @Override
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, Long userId) {
        // 校验编程语言是否合法
        Integer languageId = questionSubmitAddRequest.getLanguageId();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(languageId);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编程语言错误");
        }
        long questionId = questionSubmitAddRequest.getQuestionId();
        // 判断实体是否存在，根据类别获取实体
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 未登录不能提交
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 每个用户串行提交题目
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setUserId(userId);
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setCode(questionSubmitAddRequest.getCode());
        questionSubmit.setLanguage(languageEnum.getLanguageTitle());
        // 设置初始状态（判题状态由 status 字段承载）
        questionSubmit.setStatus(JudgeInfoMessageEnum.WAITING_JUDGE.getValue());
        boolean save = this.save(questionSubmit);
        if (!save){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据插入失败");
        }
        // 题目提交数 +1
        questionService.updateQuestionSubmitNum(questionId);
        Long questionSubmitId = questionSubmit.getId();
        // 发送消息
        myMessageProducer.sendMessage("code_exchange", "my_routingKey", String.valueOf(questionSubmitId));
        // 执行判题服务
//        CompletableFuture.runAsync(() -> {
//            judgeFeignClient.doJudge(questionSubmitId);
//        });
        return questionSubmitId;
    }


    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象，得到 mybatis 框架支持的查询 QueryWrapper 类）
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return queryWrapper;
        }
        Integer languageId = questionSubmitQueryRequest.getLanguageId();
        String language = "";
        if (languageId != null) {
            language = QuestionSubmitLanguageEnum.getEnumByValue(languageId).getLanguageTitle();
        }
        List<Long> questionIds = questionSubmitQueryRequest.getQuestionIds();
        Long userId = questionSubmitQueryRequest.getUserId();
        String sortField = questionSubmitQueryRequest.getSortField();
        String sortOrder = questionSubmitQueryRequest.getSortOrder();
        String status = questionSubmitQueryRequest.getStatus();

        // 拼接查询条件
        queryWrapper.eq(StringUtils.isNotBlank(language), "language", language);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(StringUtils.isNotBlank(status), "status", status);
        queryWrapper.in(ObjectUtils.isNotEmpty(questionIds), "questionId", questionIds);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit) {
        return getQuestionSubmitVO(questionSubmit, getAcceptedQuestionIds(GatewayHeaderUtil.getUserId()));
    }

    /**
     * 封装提交记录（传入当前用户已通过的题目集合，避免逐条查询数据库）
     *
     * @param questionSubmit
     * @param acceptedQuestionIds 当前登录用户已通过的题目 id 集合
     * @return
     */
    private QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, Set<Long> acceptedQuestionIds) {
        QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
        // 脱敏：本人、管理员或已通过该题目的用户可见代码，未登录一律隐藏
        Long currentUserId = GatewayHeaderUtil.getUserId();
        if (!canViewCode(questionSubmit, currentUserId, acceptedQuestionIds)) {
            questionSubmitVO.setCode(null);
        }
        return questionSubmitVO;
    }

    /**
     * 判断当前用户是否可见该提交的代码：本人、管理员或已通过该题目
     *
     * @param questionSubmit
     * @param currentUserId
     * @param acceptedQuestionIds
     * @return
     */
    private boolean canViewCode(QuestionSubmit questionSubmit, Long currentUserId, Set<Long> acceptedQuestionIds) {
        // 未登录一律隐藏
        if (currentUserId == null || currentUserId <= 0) {
            return false;
        }
        // 本人提交的代码
        if (currentUserId.equals(questionSubmit.getUserId())) {
            return true;
        }
        // 管理员
        if (UserRoleEnum.ADMIN.getValue().equals(GatewayHeaderUtil.getUserRole())) {
            return true;
        }
        // 已通过该题目
        return acceptedQuestionIds.contains(questionSubmit.getQuestionId());
    }

    /**
     * 查询当前用户已通过（AC）的题目 id 集合
     *
     * @param userId
     * @return
     */
    private Set<Long> getAcceptedQuestionIds(Long userId) {
        if (userId == null || userId <= 0) {
            return Collections.emptySet();
        }
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("questionId");
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("status", JudgeInfoMessageEnum.ACCEPTED.getValue());
        queryWrapper.eq("isDelete", false);
        return this.list(queryWrapper).stream()
                .map(QuestionSubmit::getQuestionId)
                .collect(Collectors.toSet());
    }

    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, List<Question> questions) {
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVO> questionSubmitVOPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());
        if (CollectionUtils.isEmpty(questionSubmitList)) {
            return questionSubmitVOPage;
        }
        Map<Long, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getId, question -> question, (o1, o2) -> o1));
        // 当前登录用户已通过的题目集合，用于判断是否可见他人代码
        Set<Long> acceptedQuestionIds = getAcceptedQuestionIds(GatewayHeaderUtil.getUserId());
        // 批量查询提交用户信息
        Set<Long> userIdSet = questionSubmitList.stream().map(QuestionSubmit::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userFeignClient.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream()
                .map(questionSubmit -> {
                    QuestionSubmitVO questionSubmitVO = getQuestionSubmitVO(questionSubmit, acceptedQuestionIds);
                    Long questionId = questionSubmit.getQuestionId();
                    Question question = questionMap.get(questionId);
                    if (question != null) {
                        questionSubmitVO.setQuestionVO(QuestionVO.objToVo(question));
                    }
                    Long userId = questionSubmit.getUserId();
                    User user = null;
                    if (userIdUserListMap.containsKey(userId)) {
                        user = userIdUserListMap.get(userId).get(0);
                    }
                    questionSubmitVO.setUserVO(userFeignClient.getUserVO(user));
                    return questionSubmitVO;
                })
                .collect(Collectors.toList());
        questionSubmitVOPage.setRecords(questionSubmitVOList);
        return questionSubmitVOPage;
    }

    @Override
    public QuestionSubmitCountVo getQuestionSubmitCount(long id) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", id);
        queryWrapper.eq("isDelete", false);
        long count = this.count(queryWrapper);
        QuestionSubmitCountVo questionSubmitCountVo = new QuestionSubmitCountVo();
        questionSubmitCountVo.setTotal(count);
        return questionSubmitCountVo;
    }

    @Override
    public QuestionSubmitCountVo getQuestionSubmitStats(long id) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", id);
        queryWrapper.eq("isDelete", false);
        queryWrapper.eq("status", JudgeInfoMessageEnum.ACCEPTED.getValue());
        List<QuestionSubmit> list = this.list(queryWrapper);
        long count = list.size();
        long passed = list.stream().map(QuestionSubmit::getQuestionId).distinct().count();
        QuestionSubmitCountVo questionSubmitCountVo = new QuestionSubmitCountVo();
        questionSubmitCountVo.setTotal(count);
        questionSubmitCountVo.setPassed(passed);
        return questionSubmitCountVo;
    }

    @Override
    public Map<Long, Integer> getQuestionStatusMap(Long userId, List<Long> questionIds) {
        if (userId == null || userId <= 0 || CollectionUtils.isEmpty(questionIds)) {
            return Collections.emptyMap();
        }
        // 查询当前用户对这批题目的提交记录（题目 id 和状态）
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("questionId", "status");
        queryWrapper.eq("userId", userId);
        queryWrapper.in("questionId", questionIds);
        queryWrapper.eq("isDelete", false);
        List<QuestionSubmit> submitList = this.list(queryWrapper);
        // 按题目聚合：任一提交通过即算通过，否则为提交未通过
        Map<Long, Boolean> questionPassedMap = new HashMap<>();
        for (QuestionSubmit submit : submitList) {
            boolean passed = JudgeInfoMessageEnum.ACCEPTED.getValue().equals(submit.getStatus());
            questionPassedMap.merge(submit.getQuestionId(), passed, (oldValue, newValue) -> oldValue || newValue);
        }
        return questionPassedMap.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue() ? QuestionStatusEnum.PASSED.getValue() : QuestionStatusEnum.FAILED.getValue()));
    }

    @Override
    public List<Long> getQuestionIdsByStatus(Long userId, Integer questionStatus) {
        QuestionStatusEnum statusEnum = QuestionStatusEnum.getEnumByValue(questionStatus);
        if (statusEnum == null) {
            return Collections.emptyList();
        }
        // 查询当前用户提交过的题目及是否通过
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("questionId", "status");
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("isDelete", false);
        List<QuestionSubmit> submitList = this.list(queryWrapper);
        Map<Long, Boolean> questionPassedMap = new HashMap<>();
        for (QuestionSubmit submit : submitList) {
            boolean passed = JudgeInfoMessageEnum.ACCEPTED.getValue().equals(submit.getStatus());
            questionPassedMap.merge(submit.getQuestionId(), passed, (oldValue, newValue) -> oldValue || newValue);
        }
        // 通过：任一提交 AC 的题目
        if (statusEnum == QuestionStatusEnum.PASSED) {
            return questionPassedMap.entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }
        // 提交未通过：提交过但从未 AC 的题目
        if (statusEnum == QuestionStatusEnum.FAILED) {
            return questionPassedMap.entrySet().stream()
                    .filter(entry -> !entry.getValue())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }
        // 未提交：题库所有题目排除提交过的
        List<Long> allQuestionIds = questionService.lambdaQuery()
                .select(Question::getId)
                .list().stream()
                .map(Question::getId)
                .collect(Collectors.toList());
        allQuestionIds.removeAll(questionPassedMap.keySet());
        return allQuestionIds;
    }

    @Override
    public MySubmitStatsVO getMySubmitStats(long userId) {
        LocalDate today = LocalDate.now();
        // 今日零点
        LocalDateTime todayStart = today.atStartOfDay();
        // 本月 1 号零点
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        // 今日提交数
        QueryWrapper<QuestionSubmit> todayWrapper = new QueryWrapper<>();
        todayWrapper.eq("userId", userId);
        todayWrapper.eq("isDelete", false);
        todayWrapper.ge("createTime", todayStart);
        long todaySubmitCount = this.count(todayWrapper);

        // 本月提交数
        QueryWrapper<QuestionSubmit> monthWrapper = new QueryWrapper<>();
        monthWrapper.eq("userId", userId);
        monthWrapper.eq("isDelete", false);
        monthWrapper.ge("createTime", monthStart);
        long monthSubmitCount = this.count(monthWrapper);

        // 本月通过数
        QueryWrapper<QuestionSubmit> acceptedWrapper = new QueryWrapper<>();
        acceptedWrapper.eq("userId", userId);
        acceptedWrapper.eq("isDelete", false);
        acceptedWrapper.ge("createTime", monthStart);
        acceptedWrapper.eq("status", JudgeInfoMessageEnum.ACCEPTED.getValue());
        long monthAcceptedCount = this.count(acceptedWrapper);

        // 连续提交天数：统计所有提交日期，若今天已提交则从今天起算，否则从昨天起算
        QueryWrapper<QuestionSubmit> historyWrapper = new QueryWrapper<>();
        historyWrapper.select("createTime");
        historyWrapper.eq("userId", userId);
        historyWrapper.eq("isDelete", false);
        Set<LocalDate> submitDates = this.list(historyWrapper).stream()
                .map(QuestionSubmit::getCreateTime)
                .filter(Objects::nonNull)
                .map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .collect(Collectors.toSet());
        long continuousSubmitDays = 0;
        LocalDate cursor = submitDates.contains(today) ? today : today.minusDays(1);
        while (submitDates.contains(cursor)) {
            continuousSubmitDays++;
            cursor = cursor.minusDays(1);
        }

        MySubmitStatsVO mySubmitStatsVO = new MySubmitStatsVO();
        mySubmitStatsVO.setTodaySubmitCount(todaySubmitCount);
        mySubmitStatsVO.setMonthSubmitCount(monthSubmitCount);
        mySubmitStatsVO.setContinuousSubmitDays(continuousSubmitDays);
        mySubmitStatsVO.setMonthAcceptedCount(monthAcceptedCount);
        return mySubmitStatsVO;
    }


}




