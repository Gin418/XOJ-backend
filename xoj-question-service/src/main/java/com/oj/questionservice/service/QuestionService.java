package com.oj.questionservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.oj.model.dto.question.QuestionQueryRequest;
import com.oj.model.entity.Question;
import com.oj.model.vo.QuestionSimpleVO;
import com.oj.model.vo.QuestionVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Gin
* @description 针对表【question(题目)】的数据库操作Service
* @createDate 2023-08-07 20:58:00
*/
public interface QuestionService extends IService<Question> {


    /**
     * 校验
     *
     * @param question
     * @param add
     */
    void validQuestion(Question question, boolean add);

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest);
    
    /**
     * 获取题目封装
     *
     * @param question
     * @param request
     * @return
     */
    QuestionVO getQuestionVO(Question question, HttpServletRequest request);

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request);

    /**
     * 按题目 id 列表查询题目简信息（题目 id、题号、标题，按传入顺序返回）
     *
     * @param ids 题目 id 列表
     * @return 题目简信息列表
     */
    List<QuestionSimpleVO> listSimpleByIds(List<Long> ids);

    /**
     * 题目提交数 +1
     *
     * @param questionId
     * @return
     */
    boolean updateQuestionSubmitNum(long questionId);

    /**
     * 题目通过数 +1
     *
     * @param questionId
     * @return
     */
    boolean updateQuestionAcceptNum(long questionId);

    /**
     * 获取每日一题（以当天日期为随机种子，同一天内题目固定，次日自动更换）
     *
     * @return 当日随机题目
     */
    Question getDailyQuestion();
}
