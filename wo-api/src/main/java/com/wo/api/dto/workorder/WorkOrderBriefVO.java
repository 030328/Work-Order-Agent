package com.wo.api.dto.workorder;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WorkOrderBriefVO implements Serializable {

    private Long id;

    private String orderNo;

    private String title;

    private String category;

    private String priority;

    private String status;

    private String assigneeName;

    private LocalDateTime createdAt;
}
