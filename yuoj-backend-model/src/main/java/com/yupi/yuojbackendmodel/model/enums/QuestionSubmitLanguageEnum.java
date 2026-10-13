package com.yupi.yuojbackendmodel.model.enums;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目提交编程语言枚举
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public enum QuestionSubmitLanguageEnum {

    JAVA(1,"Java", "java"),
    CPLUSPLUS(2, "C++", "cpp"),
    C(3, "C", "c"),
    PYTHON(4, "Python3", "python");

    private final Integer languageId;

    private final String languageTitle;

    private final String language;

    QuestionSubmitLanguageEnum(Integer languageId, String languageTitle, String language) {
        this.languageId = languageId;
        this.languageTitle = languageTitle;
        this.language = language;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<QuestionSubmitLanguageEnum> getValues() {
        return Arrays.stream(values()).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param languageId
     * @return
     */
    public static QuestionSubmitLanguageEnum getEnumByValue(Integer languageId) {
        if (languageId == null) {
            return null;
        }
        for (QuestionSubmitLanguageEnum anEnum : QuestionSubmitLanguageEnum.values()) {
            if (anEnum.languageId.equals(languageId)) {
                return anEnum;
            }
        }
        return null;
    }

    public static QuestionSubmitLanguageEnum getEnumByLanguage(String language) {
        if (StringUtils.isBlank(language)) {
            return null;
        }
        for (QuestionSubmitLanguageEnum anEnum : QuestionSubmitLanguageEnum.values()) {
            if (anEnum.languageTitle.equals(language)) {
                return anEnum;
            }
        }
        return null;
    }

    public String getLanguage() {
        return language;
    }

    public String getLanguageTitle() {
        return languageTitle;
    }

    public Integer getLanguageId() {
        return languageId;
    }
}
