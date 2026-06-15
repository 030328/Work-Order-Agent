package com.wo.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Category {

    BUG("BUG", "缺陷"),
    FEATURE("FEATURE", "需求"),
    QUESTION("QUESTION", "咨询"),
    MAINTENANCE("MAINTENANCE", "维护"),
    INCIDENT("INCIDENT", "故障");

    private final String code;
    private final String desc;
}
