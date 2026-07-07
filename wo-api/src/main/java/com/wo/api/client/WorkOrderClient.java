package com.wo.api.client;

import com.wo.api.client.fallback.WorkOrderClientFallback;
import com.wo.api.config.InternalFeignConfig;
import com.wo.api.dto.workorder.*;
import com.wo.common.result.PageResult;
import com.wo.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "wo-service-workorder", fallbackFactory = WorkOrderClientFallback.class, configuration = InternalFeignConfig.class)
public interface WorkOrderClient {

    @GetMapping("/api/workorders/{id}")
    R<WorkOrderVO> getWorkOrder(@PathVariable("id") Long id);

    @GetMapping("/api/workorders")
    R<PageResult<WorkOrderBriefVO>> queryWorkOrders(@SpringQueryMap WorkOrderQueryDTO query);

    @PostMapping("/api/workorders")
    R<WorkOrderVO> createWorkOrder(@RequestBody WorkOrderCreateDTO dto);

    @PutMapping("/api/workorders/{id}/status")
    R<Void> updateWorkOrderStatus(@PathVariable("id") Long id,
                                  @RequestBody WorkOrderStatusUpdateDTO dto);

    @PutMapping("/api/workorders/{id}/assign")
    R<Void> assignWorkOrder(@PathVariable("id") Long id,
                            @RequestBody WorkOrderAssignDTO dto);

    @PutMapping("/api/workorders/{id}/reject")
    R<Void> rejectResolution(@PathVariable("id") Long id,
                             @RequestBody WorkOrderRejectDTO dto);

    @GetMapping("/api/workorders/{id}/flows")
    R<List<FlowRecordVO>> listFlowRecords(@PathVariable("id") Long id);

    @GetMapping("/api/workorders/{workOrderId}/comments")
    R<List<CommentVO>> getComments(@PathVariable("workOrderId") Long workOrderId);

    @PostMapping("/api/workorders/{workOrderId}/comments")
    R<CommentVO> addComment(@PathVariable("workOrderId") Long workOrderId,
                            @RequestBody CommentCreateDTO dto);

    @PutMapping("/api/workorders/internal/{id}/sla-deadline")
    R<Void> updateSlaDeadline(@PathVariable("id") Long id,
                              @RequestParam("deadline") String deadline);

    @GetMapping("/api/workorders/internal/sla-breached")
    R<List<Long>> listSlaBreachedWorkOrderIds(@RequestParam("deadlineBefore") String deadlineBefore);

    @PutMapping("/api/workorders/internal/{id}/sla-breach")
    R<Void> markSlaBreached(@PathVariable("id") Long id);
}
