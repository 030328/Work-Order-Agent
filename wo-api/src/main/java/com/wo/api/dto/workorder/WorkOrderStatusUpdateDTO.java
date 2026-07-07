package com.wo.api.dto.workorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class WorkOrderStatusUpdateDTO implements Serializable {

    @NotBlank(message = "status is required")
    private String status;

    @Size(max = 1000, message = "comment must not exceed 1000 characters")
    private String comment;
}
