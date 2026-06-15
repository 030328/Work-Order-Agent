package com.wo.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(0, "success"),
    SYSTEM_ERROR(10000, "系统错误"),
    PARAM_ERROR(10001, "参数错误"),
    UNAUTHORIZED(10002, "未授权"),
    FORBIDDEN(10003, "无权限"),
    NOT_FOUND(10004, "资源不存在"),
    CONFLICT(10005, "数据冲突"),

    // User errors
    USER_NOT_FOUND(20001, "用户不存在"),
    USER_PASSWORD_ERROR(20002, "密码错误"),
    USER_DISABLED(20003, "用户已禁用"),
    USER_ALREADY_EXISTS(20004, "用户已存在"),

    // Work order errors
    WO_NOT_FOUND(30001, "工单不存在"),
    WO_STATUS_INVALID(30002, "工单状态不允许此操作"),
    WO_ALREADY_ASSIGNED(30003, "工单已分配"),

    // Workflow errors
    WF_TRANSITION_INVALID(40001, "不允许的状态转换"),
    WF_DEFINITION_NOT_FOUND(40002, "工作流定义不存在"),

    // AI errors
    AI_SERVICE_ERROR(50001, "AI服务调用失败"),
    AI_SESSION_NOT_FOUND(50002, "会话不存在"),
    KNOWLEDGE_INDEX_ERROR(50003, "知识库索引失败");

    private final int code;
    private final String msg;
}
