package com.wo.api.dto.workorder;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CommentVO implements Serializable {

    private Long id;

    private Long workOrderId;

    private Long userId;

    private String userName;

    private String content;

    private Boolean isInternal;

    private Boolean isAiGenerated;

    private LocalDateTime createdAt;
}
