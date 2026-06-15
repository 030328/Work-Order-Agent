package com.wo.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRole {

    ADMIN("ADMIN", "管理员"),
    MANAGER("MANAGER", "经理"),
    AGENT("AGENT", "客服"),
    USER("USER", "普通用户");

    private final String code;
    private final String desc;
}
