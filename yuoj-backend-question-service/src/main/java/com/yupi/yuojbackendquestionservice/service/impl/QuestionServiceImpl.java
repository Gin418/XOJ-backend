package com.yupi.yuojbackendquestionservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.constant.CommonConstant;
import com.yupi.yuojbackendcommon.exception.BusinessException;
import com.yupi.yuojbackendcommon.exception.ThrowUtils;
import com.yupi.yuojbackendcommon.utils.GatewayHeaderUtil;
import com.yupi.yuojbackendcommon.utils.SqlUtils;
import com.yupi.yuojbackendmodel.model.dto.question.QuestionQueryRequest;
import com.yupi.yuojbackendmodel.model.entity.Question;
import com.yupi.yuojbackendmodel.model.entity.User;
import com.yupi.yuojbackendmodel.model.enums.QuestionStatusEnum;
import com.yupi.yuojbackendmodel.model.vo.QuestionSimpleVO;
import com.yupi.yuojbackendmodel.model.vo.QuestionVO;
import com.yupi.yuojbackendmodel.model.vo.UserVO;
import com.yupi.yuojbackendquestionservice.mapper.QuestionMapper;
import com.yupi.yuojbackendquestionservice.service.QuestionService;
import com.yupi.yuojbackendquestionservice.service.QuestionSubmitService;
import com.yupi.yuojbackendserviceclient.service.UserFeignClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author 李鱼皮
* @description 针对表【question(题目)】的数据库操作Service实现
* @createDate 2023-08-07 20:58:00
*/
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question>
    implements QuestionService {

    /**
     * 每日一题 Redis key 前缀（key 格式：question:daily:{yyyy-MM-dd}）
     */
    private static final String DAILY_QUESTION_KEY_PREFIX = "question:daily:";

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    @Lazy
    private QuestionSubmitService questionSubmitService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 校验题目是否合法
     * @param question
     * @param add
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        if (question == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = question.getTitle();
        String customId = question.getCustomId();
        String content = question.getContent();
        String tags = question.getTags();
        String description = question.getDescription();
        String judgeCase = question.getJudgeCase();
        String judgeConfig = question.getJudgeConfig();
        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isAnyBlank(customId, title, content, tags), ErrorCode.PARAMS_ERROR);
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(title) && title.length() > 80) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
        if (StringUtils.isNotBlank(content) && content.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容过长");
        }
        if (StringUtils.isNotBlank(description) && description.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目说明过长");
        }
        if (StringUtils.isNotBlank(judgeCase) && judgeCase.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "判题用例过长");
        }
        if (StringUtils.isNotBlank(judgeConfig) && judgeConfig.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "判题配置过长");
        }
    }

    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象，得到 mybatis 框架支持的查询 QueryWrapper 类）
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        Long id = questionQueryRequest.getId();
        String customId = questionQueryRequest.getCustomId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        List<String> tags = questionQueryRequest.getTags();
        String description = questionQueryRequest.getDescription();
        Long userId = questionQueryRequest.getUserId();
        Integer questionStatus = questionQueryRequest.getQuestionStatus();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();
        String keyword = questionQueryRequest.getKeyword();

        // 拼接查询条件（keyword 同时模糊匹配标题和题号）
        queryWrapper.and(StringUtils.isNotBlank(keyword), queryWrapper1 -> queryWrapper1
                .like("title", keyword)
                .or()
                .like("customId", keyword));
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.like(StringUtils.isNotBlank(description), "description", description);
        if (CollectionUtils.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.like(StringUtils.isNotBlank(customId), "customId", customId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 当前用户做题状态过滤（QuestionStatusEnum：0-未提交 1-提交未通过 2-通过）
        if (ObjectUtils.isNotEmpty(questionStatus)) {
            List<Long> statusQuestionIds = questionSubmitService.getQuestionIdsByStatus(GatewayHeaderUtil.getUserId(), questionStatus);
            if (CollectionUtils.isEmpty(statusQuestionIds)) {
                // 无匹配题目，强制查询为空
                queryWrapper.eq("id", -1);
            } else {
                queryWrapper.in("id", statusQuestionIds);
            }
        }
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        QuestionVO questionVO = QuestionVO.objToVo(question);
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userFeignClient.getById(userId);
        }
        UserVO userVO = userFeignClient.getUserVO(user);
        questionVO.setUserVO(userVO);
        return questionVO;
    }

    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollectionUtils.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userFeignClient.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 查询当前用户对这批题目的做题状态
        Long currentUserId = GatewayHeaderUtil.getUserId();
        List<Long> questionIdList = questionList.stream().map(Question::getId).collect(Collectors.toList());
        Map<Long, Integer> questionStatusMap = questionSubmitService.getQuestionStatusMap(currentUserId, questionIdList);
        // 填充信息
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            QuestionVO questionVO = QuestionVO.objToVo(question);
            Long userId = question.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUserVO(userFeignClient.getUserVO(user));
            questionVO.setQuestionStatus(questionStatusMap.getOrDefault(question.getId(), QuestionStatusEnum.NOT_SUBMITTED.getValue()));
            return questionVO;
        }).collect(Collectors.toList());
        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    @Override
    public List<QuestionSimpleVO> listSimpleByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        Map<Long, Question> questionMap = this.listByIds(ids).stream()
                .collect(Collectors.toMap(Question::getId, question -> question, (a, b) -> a));
        // 按传入的 id 顺序返回
        return ids.stream()
                .map(questionMap::get)
                .filter(Objects::nonNull)
                .map(question -> {
                    QuestionSimpleVO simpleVO = new QuestionSimpleVO();
                    simpleVO.setId(question.getId());
                    simpleVO.setCustomId(question.getCustomId());
                    simpleVO.setTitle(question.getTitle());
                    return simpleVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateQuestionSubmitNum(long questionId) {
        UpdateWrapper<Question> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", questionId).setSql("submitNum = submitNum + 1");
        return this.update(updateWrapper);
    }

    @Override
    public boolean updateQuestionAcceptNum(long questionId) {
        UpdateWrapper<Question> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", questionId).setSql("acceptedNum = acceptedNum + 1");
        return this.update(updateWrapper);
    }

    @Override
    public Question getDailyQuestion() {
        String today = LocalDate.now().toString();
        String redisKey = DAILY_QUESTION_KEY_PREFIX + today;
        // 优先从 Redis 读取当天已选定的题目 id
        String cachedQuestionId = stringRedisTemplate.opsForValue().get(redisKey);
        if (StringUtils.isNotBlank(cachedQuestionId)) {
            Question cachedQuestion = this.getById(Long.valueOf(cachedQuestionId));
            if (cachedQuestion != null) {
                return cachedQuestion;
            }
            // 缓存的题目已不存在（如被删除），删除缓存后重新随机
            stringRedisTemplate.delete(redisKey);
        }
        // 查询所有未删除题目的 id（按 id 升序，保证列表稳定）
        List<Long> questionIds = this.lambdaQuery()
                .select(Question::getId)
                .orderByAsc(Question::getId)
                .list().stream()
                .map(Question::getId)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(questionIds)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题库为空");
        }
        // 以当天日期为随机种子：同一天所有用户看到同一题，次日自动更换
        long seed = LocalDate.now().toEpochDay();
        int index = new Random(seed).nextInt(questionIds.size());
        Question dailyQuestion = this.getById(questionIds.get(index));
        // 写入 Redis 缓存，当天有效（次日零点过期）
        if (dailyQuestion != null) {
            Duration expireDuration = Duration.between(
                    LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay());
            stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(dailyQuestion.getId()), expireDuration);
        }
        return dailyQuestion;
    }


}




