package com.wo.api.client;

import com.wo.api.client.fallback.WorkOrderClientFallback;
import com.wo.api.dto.workorder.*;
import com.wo.common.result.PageResult;
import com.wo.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "wo-service-workorder", fallbackFactory = WorkOrderClientFallback.class)
public interface WorkOrderClient {

    @GetMapping("/api/workorders/{id}")
    R<WorkOrderVO> getWorkOrder(@PathVariable("id") Long id);

    @PostMapping("/api/workorders/query")
    R<PageResult<WorkOrderBriefVO>> queryWorkOrders(@RequestBody WorkOrderQueryDTO query);

    @PostMapping("/api/workorders")
    R<WorkOrderVO> createWorkOrder(@RequestBody WorkOrderCreateDTO dto);

    @PutMapping("/api/workorders/{id}/status")
    R<Void> updateWorkOrderStatus(@PathVariable("id") Long id,
                                  @RequestParam("status") String status,
                                  @RequestParam(value = "comment", required = false) String comment);

    @PutMapping("/api/workorders/{id}/assign")
    R<Void> assignWorkOrder(@PathVariable("id") Long id,
                            @RequestParam("assigneeId") Long assigneeId,
                            @RequestParam(value = "reason", required = false) String reason);

    @GetMapping("/api/workorders/{workOrderId}/comments")
    R<List<CommentVO>> getComments(@PathVariable("workOrderId") Long workOrderId);

    @PostMapping("/api/workorders/{workOrderId}/comments")
    R<Void> addComment(@PathVariable("workOrderId") Long workOrderId,
                       @RequestBody CommentCreateDTO dto);
}
