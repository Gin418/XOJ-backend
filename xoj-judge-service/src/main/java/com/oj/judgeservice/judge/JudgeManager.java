package com.oj.judgeservice.judge;

import com.oj.judgeservice.judge.strategy.DefaultJudgeStrategy;
import com.oj.judgeservice.judge.strategy.JavaLanguageJudgeStrategy;
import com.oj.judgeservice.judge.strategy.JudgeContext;
import com.oj.judgeservice.judge.strategy.JudgeStrategy;
import com.oj.model.codesandbox.JudgeInfo;
import com.oj.model.entity.QuestionSubmit;
import org.springframework.stereotype.Service;

/**
 * 判题管理（简化调用）
 */
@Service
public class JudgeManager {

    /**
     * 执行判题
     *
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext) {
        QuestionSubmit questionSubmit = judgeContext.getQuestionSubmit();
        String language = questionSubmit.getLanguage();
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        if ("Java".equals(language)) {
            judgeStrategy = new JavaLanguageJudgeStrategy();
        }
        return judgeStrategy.doJudge(judgeContext);
    }

}
