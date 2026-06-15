package com.wo.api.dto.workorder;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WorkOrderQueryDTO implements Serializable {

    private String keyword;

    private String status;

    private String priority;

    private String category;

    private Long assigneeId;

    private Long creatorId;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Integer page = 1;

    private Integer size = 10;
}
