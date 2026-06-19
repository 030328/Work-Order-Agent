package com.wo.api.dto.workorder;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkOrderVO implements Serializable {

    private Long id;

    private String orderNo;

    private String title;

    private String description;

    private String category;

    private String priority;

    private String status;

    private Long creatorId;

    private String creatorName;

    private Long assigneeId;

    private String assigneeName;

    private String department;

    private LocalDateTime slaDeadline;

    private LocalDateTime resolvedAt;

    private LocalDateTime closedAt;

    private String resolution;

    private List<String> tags;

    private String aiSummary;

    private String aiSentiment;

    private String aiCategorySuggestion;

    private String aiSuggestedSolution;

    private LocalDateTime escalatedAt;

    private LocalDateTime claimedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
