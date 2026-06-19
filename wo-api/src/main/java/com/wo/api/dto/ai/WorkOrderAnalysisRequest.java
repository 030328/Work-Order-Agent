package com.wo.api.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

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

    /**
     * ES 检索的相似历史工单（workorder 服务传过来）
     */
    private List<SimilarWorkOrder> similarWorkOrders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimilarWorkOrder implements Serializable {
        private Long id;
        private String orderNo;
        private String title;
        private String description;
        private String resolution;
        private String status;
    }
}
