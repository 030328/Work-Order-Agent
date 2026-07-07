package com.wo.workorder.controller;

import com.wo.api.dto.workorder.*;
import com.wo.common.result.PageResult;
import com.wo.common.result.R;
import com.wo.common.security.InternalServiceAuth;
import com.wo.common.util.SecurityUtil;
import com.wo.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workorders")
@RequiredArgsConstructor
@Validated
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final InternalServiceAuth internalServiceAuth;

    /**
     * 获取工单详情
     */
    @GetMapping("/{id}")
    public R<WorkOrderVO> getWorkOrder(@PathVariable @Positive Long id) {
        WorkOrderVO vo = workOrderService.getWorkOrder(id);
        return R.ok(vo);
    }

    /**
     * 分页查询工单
     */
    @GetMapping
    public R<PageResult<WorkOrderBriefVO>> queryWorkOrders(@Valid WorkOrderQueryDTO query) {
        PageResult<WorkOrderBriefVO> result = workOrderService.queryWorkOrders(query);
        return R.ok(result);
    }

    /**
     * 按状态统计工单数量
     */
    @GetMapping("/stats/status")
    public R<Map<String, Long>> countWorkOrdersByStatus(@Valid WorkOrderQueryDTO query) {
        return R.ok(workOrderService.countWorkOrdersByStatus(query));
    }

    /**
     * 创建工单
     */
    @PostMapping
    public R<WorkOrderVO> createWorkOrder(@Valid @RequestBody WorkOrderCreateDTO dto) {
        Long userId = SecurityUtil.requireCurrentUserId();
        WorkOrderVO vo = workOrderService.createWorkOrder(dto, userId);
        return R.ok(vo);
    }

    /**
     * 更新工单状态
     */
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable @Positive Long id,
                                @Valid @RequestBody WorkOrderStatusUpdateDTO dto) {
        Long operatorId = SecurityUtil.requireCurrentUserId();
        workOrderService.updateStatus(id, dto.getStatus(), operatorId, dto.getComment());
        return R.ok();
    }

    /**
     * 分配工单
     */
    @PutMapping("/{id}/assign")
    public R<Void> assignWorkOrder(@PathVariable @Positive Long id,
                                   @Valid @RequestBody WorkOrderAssignDTO dto) {
        Long operatorId = SecurityUtil.requireCurrentUserId();
        workOrderService.assignWorkOrder(id, dto.getAssigneeId(), operatorId, dto.getReason());
        return R.ok();
    }

    /**
     * 用户确认工单完成
     */
    @PutMapping("/{id}/confirm")
    public R<Void> confirmWorkOrder(@PathVariable @Positive Long id) {
        Long userId = SecurityUtil.requireCurrentUserId();
        workOrderService.confirmWorkOrder(id, userId);
        return R.ok();
    }

    @PutMapping("/{id}/reject")
    public R<Void> rejectResolution(@PathVariable @Positive Long id,
                                    @Valid @RequestBody WorkOrderRejectDTO dto) {
        Long userId = SecurityUtil.requireCurrentUserId();
        workOrderService.rejectResolution(id, userId, dto.getReason());
        return R.ok();
    }

    /**
     * AI 重新生成解决方案
     */
    @PostMapping("/{id}/regenerate")
    public R<WorkOrderVO> regenerateSolution(@PathVariable @Positive Long id) {
        Long userId = SecurityUtil.requireCurrentUserId();
        WorkOrderVO vo = workOrderService.regenerateSolution(id, userId);
        return R.ok(vo);
    }

    /**
     * 转人工处理
     */
    @PutMapping("/{id}/escalate")
    public R<Void> escalateWorkOrder(@PathVariable @Positive Long id) {
        Long userId = SecurityUtil.requireCurrentUserId();
        workOrderService.escalateWorkOrder(id, userId);
        return R.ok();
    }

    /**
     * 认领工单（部门人员）
     */
    @PutMapping("/{id}/claim")
    public R<Void> claimWorkOrder(@PathVariable @Positive Long id) {
        Long userId = SecurityUtil.requireCurrentUserId();
        workOrderService.claimWorkOrder(id, userId);
        return R.ok();
    }

    /**
     * 获取部门待处理工单列表
     */
    @GetMapping("/department/{department}")
    public R<PageResult<WorkOrderBriefVO>> getDepartmentWorkOrders(
            @PathVariable String department,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        PageResult<WorkOrderBriefVO> result = workOrderService.getDepartmentWorkOrders(department, page, size);
        return R.ok(result);
    }

    @GetMapping("/{id}/flows")
    public R<List<FlowRecordVO>> listFlowRecords(@PathVariable @Positive Long id) {
        return R.ok(workOrderService.listFlowRecords(id));
    }

    @PutMapping("/internal/{id}/sla-deadline")
    public R<Void> updateSlaDeadline(@PathVariable @Positive Long id,
                                     @RequestParam String deadline,
                                     jakarta.servlet.http.HttpServletRequest request) {
        internalServiceAuth.require(request);
        workOrderService.updateSlaDeadline(id, LocalDateTime.parse(deadline));
        return R.ok();
    }

    @GetMapping("/internal/sla-breached")
    public R<List<Long>> listSlaBreachedWorkOrderIds(@RequestParam String deadlineBefore,
                                                     jakarta.servlet.http.HttpServletRequest request) {
        internalServiceAuth.require(request);
        List<Long> ids = workOrderService.listSlaBreachedWorkOrderIds(LocalDateTime.parse(deadlineBefore));
        return R.ok(ids);
    }

    @PutMapping("/internal/{id}/sla-breach")
    public R<Void> markSlaBreached(@PathVariable @Positive Long id,
                                   jakarta.servlet.http.HttpServletRequest request) {
        internalServiceAuth.require(request);
        workOrderService.markSlaBreached(id);
        return R.ok();
    }
}
