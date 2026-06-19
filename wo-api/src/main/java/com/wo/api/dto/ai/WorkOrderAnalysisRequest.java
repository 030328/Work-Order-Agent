package com.wo.api.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderAnalysisRequest implements Serializable {

    private Long workOrderId;
    private String title;
    private String description;
    private String category;
    private String priority;
}
