package com.yupi.yuojbackendmodel.model.enums;

public enum CompetitionStatusEnum {

    PENDING("NOT_START", "待开始"),
    ONGOING("RUNNING", "进行中"),
    ENDED("END", "已结束");

    private final String code;
    private final String description;

    CompetitionStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
