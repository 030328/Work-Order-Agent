package com.wo.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkOrderStatus {

    DRAFT("DRAFT", "草稿"),
    OPEN("OPEN", "待分配"),
    IN_PROGRESS("IN_PROGRESS", "处理中"),
    PENDING_REVIEW("PENDING_REVIEW", "待验收"),
    RESOLVED("RESOLVED", "已解决"),
    CLOSED("CLOSED", "已关闭"),
    REJECTED("REJECTED", "已驳回");

    private final String code;
    private final String desc;
}
