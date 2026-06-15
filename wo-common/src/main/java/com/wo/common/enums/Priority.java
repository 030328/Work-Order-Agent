package com.wo.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Priority {

    LOW("LOW", "低", 1),
    MEDIUM("MEDIUM", "中", 2),
    HIGH("HIGH", "高", 3),
    URGENT("URGENT", "紧急", 4);

    private final String code;
    private final String desc;
    private final int level;
}
