package com.oj.model.enums;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 判题信息消息枚举
 */
public enum JudgeInfoMessageEnum {

    COMPILING("Compiling", "编译中"),
    WAITING_JUDGE("Waiting_Judge", "等待判题"),
    JUDGING("Judging", "判题中"),
    ACCEPTED("Accepted", "答案正确"),
    WRONG_ANSWER("Wrong_Answer", "答案错误"),
    COMPILE_ERROR("Compile_Error", "编译错误"),
    MEMORY_LIMIT_EXCEEDED("Memory_Limit_Exceeded", "内存溢出"),
    TIME_LIMIT_EXCEEDED("Time_Limit_Exceeded", "运行超时"),
    SIGNALLED("Signalled", "运行错误"),
    PRESENTATION_ERROR("Presentation_Error", "格式错误"),
    NONZERO_EXIT_STATUS("Nonzero_Exit_Status", "非零返回"),
    OUTPUT_LIMIT_EXCEEDED("Output_Limit_Exceeded", "输出溢出"),
    INTERNAL_ERROR("Internal_Error", "系统错误"),
    FILE_ERROR("File_Error", "文件错误"),
    UNKNOWN("Unknown", "未知结果");

    private final String value;

    private final String text;

    JudgeInfoMessageEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static JudgeInfoMessageEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (JudgeInfoMessageEnum anEnum : JudgeInfoMessageEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
