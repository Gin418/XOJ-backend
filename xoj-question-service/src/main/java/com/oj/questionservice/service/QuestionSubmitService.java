package com.oj.questionservice.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.oj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.oj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.oj.model.entity.Question;
import com.oj.model.entity.QuestionSubmit;
import com.oj.model.vo.DailyQuestionRankItemVO;
import com.oj.model.vo.DailySubmitStatsVO;
import com.oj.model.vo.MySubmitStatsVO;
import com.oj.model.vo.QuestionSubmitCountVo;
import com.oj.model.vo.QuestionSubmitVO;

import java.util.List;
import java.util.Map;

/**
* @author Gin
* @description 针对表【question_submit(题目提交)】的数据库操作Service
* @createDate 2023-08-07 20:58:53
*/
public interface QuestionSubmitService extends IService<QuestionSubmit> {
    
    /**
     * 题目提交
     *
     * @param questionSubmitAddRequest 题目提交信息
     * @param userId
     * @return
     */
    long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, Long userId);

    /**
     * 获取查询条件
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest);

    /**
     * 获取题目封装
     *
     * @param questionSubmit
     * @param loginUser
     * @return
     */
    QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit);

    /**
     * 分页获取题目封装
     *
     * @param questionSubmitPage
     * @param loginUser
     * @return
     */
    Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, List<Question> questions);

    QuestionSubmitCountVo getQuestionSubmitCount(long id);

    QuestionSubmitCountVo getQuestionSubmitStats(long id);

    /**
     * 查询当前用户对指定题目的做题状态（QuestionStatusEnum 状态值）
     *
     * @param userId
     * @param questionIds
     * @return questionId -> 状态 value 的映射，未登录或无提交返回空 map
     */
    Map<Long, Integer> getQuestionStatusMap(Long userId, List<Long> questionIds);

    /**
     * 查询当前用户处于指定状态（通过 / 提交未通过 / 未提交）的题目 id 列表
     *
     * @param userId
     * @param questionStatus QuestionStatusEnum 状态值
     * @return 匹配的题目 id 列表；未登录时 PASSED/FAILED 返回空列表，NOT_SUBMITTED 返回全部题目
     */
    List<Long> getQuestionIdsByStatus(Long userId, Integer questionStatus);

    /**
     * 获取我的提交统计（今日提交数、本月提交数、连续提交天数、本月通过数）
     *
     * @param userId
     * @return
     */
    MySubmitStatsVO getMySubmitStats(long userId);

    /**
     * 获取最近 N 天的每日提交次数和 AC 次数（含今天，无提交的日期补 0）
     *
     * @param userId
     * @param days 统计天数（含今天）
     * @return 按日期升序的每日统计列表
     */
    List<DailySubmitStatsVO> getRecentDailySubmitStats(long userId, int days);

    /**
     * 获取每日一题解题榜（当天在每日一题上首次通过时间最早的前 N 名）
     *
     * @param limit 榜单人数
     * @return 按通过时间升序的榜单列表
     */
    List<DailyQuestionRankItemVO> getDailyQuestionRank(int limit);
}
