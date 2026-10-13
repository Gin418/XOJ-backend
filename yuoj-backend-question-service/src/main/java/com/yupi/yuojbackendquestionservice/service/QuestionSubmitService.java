package com.yupi.yuojbackendquestionservice.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yuojbackendmodel.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.yupi.yuojbackendmodel.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.yupi.yuojbackendmodel.model.entity.Question;
import com.yupi.yuojbackendmodel.model.entity.QuestionSubmit;
import com.yupi.yuojbackendmodel.model.vo.MySubmitStatsVO;
import com.yupi.yuojbackendmodel.model.vo.QuestionSubmitCountVo;
import com.yupi.yuojbackendmodel.model.vo.QuestionSubmitVO;

import java.util.List;
import java.util.Map;

/**
* @author 李鱼皮
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
}
