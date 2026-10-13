package com.yupi.yuojbackendmodel.model.enums;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 当前用户题目状态枚举（通过 / 提交未通过 / 未提交）
 *
 * @author Q
 * @Date 2026/10/12 15:00
 * @Version 1.0.0
 */
public enum QuestionStatusEnum {

    NOT_SUBMITTED(0, "未提交"),
    FAILED(1, "提交未通过"),
    PASSED(2, "通过");

    private final Integer value;

    private final String text;

    QuestionStatusEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static QuestionStatusEnum getEnumByValue(Integer value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (QuestionStatusEnum anEnum : QuestionStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public Integer getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
