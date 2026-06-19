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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
     * 按状态统计工单数量
     */
    @GetMapping("/stats/status")
    public R<Map<String, Long>> countWorkOrdersByStatus(WorkOrderQueryDTO query) {
        return R.ok(workOrderService.countWorkOrdersByStatus(query));
    }

    /**
     * 创建工单
     */
    @PostMapping
    public R<WorkOrderVO> createWorkOrder(@Valid @RequestBody WorkOrderCreateDTO dto,
                                          @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = userIdHeader != null ? Long.valueOf(userIdHeader) : SecurityUtil.getCurrentUserId();
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

    /**
     * 用户确认工单完成
     */
    @PutMapping("/{id}/confirm")
    public R<Void> confirmWorkOrder(@PathVariable Long id,
                                    @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = userIdHeader != null ? Long.valueOf(userIdHeader) : SecurityUtil.getCurrentUserId();
        workOrderService.confirmWorkOrder(id, userId);
        return R.ok();
    }

    /**
     * AI 重新生成解决方案
     */
    @PostMapping("/{id}/regenerate")
    public R<WorkOrderVO> regenerateSolution(@PathVariable Long id,
                                             @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = userIdHeader != null ? Long.valueOf(userIdHeader) : SecurityUtil.getCurrentUserId();
        WorkOrderVO vo = workOrderService.regenerateSolution(id, userId);
        return R.ok(vo);
    }

    /**
     * 转人工处理
     */
    @PutMapping("/{id}/escalate")
    public R<Void> escalateWorkOrder(@PathVariable Long id,
                                     @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = userIdHeader != null ? Long.valueOf(userIdHeader) : SecurityUtil.getCurrentUserId();
        workOrderService.escalateWorkOrder(id, userId);
        return R.ok();
    }

    /**
     * 认领工单（部门人员）
     */
    @PutMapping("/{id}/claim")
    public R<Void> claimWorkOrder(@PathVariable Long id,
                                  @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = userIdHeader != null ? Long.valueOf(userIdHeader) : SecurityUtil.getCurrentUserId();
        workOrderService.claimWorkOrder(id, userId);
        return R.ok();
    }

    /**
     * 获取部门待处理工单列表
     */
    @GetMapping("/department/{department}")
    public R<PageResult<WorkOrderBriefVO>> getDepartmentWorkOrders(
            @PathVariable String department,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<WorkOrderBriefVO> result = workOrderService.getDepartmentWorkOrders(department, page, size);
        return R.ok(result);
    }

    @PutMapping("/internal/{id}/sla-deadline")
    public R<Void> updateSlaDeadline(@PathVariable Long id, @RequestParam String deadline) {
        workOrderService.updateSlaDeadline(id, LocalDateTime.parse(deadline));
        return R.ok();
    }

    @GetMapping("/internal/sla-breached")
    public R<List<Long>> listSlaBreachedWorkOrderIds(@RequestParam String deadlineBefore) {
        List<Long> ids = workOrderService.listSlaBreachedWorkOrderIds(LocalDateTime.parse(deadlineBefore));
        return R.ok(ids);
    }

    @PutMapping("/internal/{id}/sla-breach")
    public R<Void> markSlaBreached(@PathVariable Long id) {
        workOrderService.markSlaBreached(id);
        return R.ok();
    }
}
