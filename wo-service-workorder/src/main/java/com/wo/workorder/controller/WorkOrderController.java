package com.wo.workorder.controller;

import com.wo.api.dto.workorder.WorkOrderBriefVO;
import com.wo.api.dto.workorder.WorkOrderCreateDTO;
import com.wo.api.dto.workorder.WorkOrderQueryDTO;
import com.wo.api.dto.workorder.WorkOrderVO;
import com.wo.common.result.PageResult;
import com.wo.common.result.R;
import com.wo.common.util.SecurityUtil;
import com.wo.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workorders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    /**
     * 获取工单详情
     */
    @GetMapping("/{id}")
    public R<WorkOrderVO> getWorkOrder(@PathVariable Long id) {
        WorkOrderVO vo = workOrderService.getWorkOrder(id);
        return R.ok(vo);
    }

    /**
     * 分页查询工单
     */
    @GetMapping
    public R<PageResult<WorkOrderBriefVO>> queryWorkOrders(WorkOrderQueryDTO query) {
        PageResult<WorkOrderBriefVO> result = workOrderService.queryWorkOrders(query);
        return R.ok(result);
    }

    /**
     * 创建工单
     */
    @PostMapping
    public R<WorkOrderVO> createWorkOrder(@Valid @RequestBody WorkOrderCreateDTO dto,
                                          @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = userIdHeader != null ? Long.parseLong(userIdHeader) : SecurityUtil.getCurrentUserId();
        if (userId == null) {
            userId = 0L; // 默认用户
        }
        WorkOrderVO vo = workOrderService.createWorkOrder(dto, userId);
        return R.ok(vo);
    }

    /**
     * 更新工单状态
     */
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id,
                                @RequestParam String status,
                                @RequestParam(required = false) String comment) {
        Long operatorId = SecurityUtil.getCurrentUserId();
        workOrderService.updateStatus(id, status, operatorId, comment);
        return R.ok();
    }

    /**
     * 分配工单
     */
    @PutMapping("/{id}/assign")
    public R<Void> assignWorkOrder(@PathVariable Long id,
                                   @RequestParam Long assigneeId,
                                   @RequestParam(required = false) String reason) {
        Long operatorId = SecurityUtil.getCurrentUserId();
        workOrderService.assignWorkOrder(id, assigneeId, operatorId, reason);
        return R.ok();
    }
}
