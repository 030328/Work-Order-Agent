package com.wo.api.dto.workorder;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FlowRecordVO implements Serializable {

    private Long id;

    private Long workOrderId;

    private String action;

    private String fromStatus;

    private String toStatus;

    private Long operatorId;

    private String operatorName;

    private String comment;

    private String attachmentUrls;

    private Boolean isSystem;

    private LocalDateTime createdAt;
}
