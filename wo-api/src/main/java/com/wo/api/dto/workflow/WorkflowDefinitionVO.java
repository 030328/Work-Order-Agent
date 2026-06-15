package com.wo.api.dto.workflow;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WorkflowDefinitionVO implements Serializable {

    private Long id;

    private String name;

    private String description;

    private Integer version;

    private String status;

    private LocalDateTime createdAt;
}
