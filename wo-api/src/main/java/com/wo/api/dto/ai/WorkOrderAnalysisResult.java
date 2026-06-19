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
public class WorkOrderAnalysisResult implements Serializable {

    private String suggestedCategory;
    private String suggestedPriority;
    private String summary;
    private String sentiment;
    private String suggestedSolution;
}
