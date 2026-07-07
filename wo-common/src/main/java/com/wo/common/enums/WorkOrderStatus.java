package com.wo.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkOrderStatus {

    OPEN("OPEN", "待处理"),
    AI_ANALYZING("AI_ANALYZING", "AI分析中"),
    AI_SOLVED("AI_SOLVED", "AI已处理"),
    ESCALATED("ESCALATED", "已转人工"),
    IN_PROGRESS("IN_PROGRESS", "处理中"),
    RESOLVED("RESOLVED", "已解决"),
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String desc;

    public static boolean isValid(String code) {
        for (WorkOrderStatus status : values()) {
            if (status.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
}
