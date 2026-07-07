package com.wo.api.dto.workorder;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @Min(value = 1, message = "page must be greater than or equal to 1")
    private Integer page = 1;

    @Min(value = 1, message = "size must be greater than or equal to 1")
    @Max(value = 100, message = "size must not exceed 100")
    private Integer size = 10;
}
