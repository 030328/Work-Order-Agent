package com.wo.common.enums;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class WorkOrderStatusTest {

    @Test
    void statusCodesShouldBeUnique() {
        long uniqueCodeCount = Arrays.stream(WorkOrderStatus.values())
                .map(WorkOrderStatus::getCode)
                .distinct()
                .count();

        assertThat(uniqueCodeCount).isEqualTo(WorkOrderStatus.values().length);
    }

    @Test
    void statusShouldExposeReadableDescription() {
        assertThat(WorkOrderStatus.OPEN.getCode()).isEqualTo("OPEN");
        assertThat(WorkOrderStatus.OPEN.getDesc()).isEqualTo("待处理");
        assertThat(WorkOrderStatus.CLOSED.getDesc()).isEqualTo("已关闭");
    }
}
