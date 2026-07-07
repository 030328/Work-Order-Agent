package com.wo.api.client.fallback;

import com.wo.api.client.WorkOrderClient;
import com.wo.api.dto.workorder.*;
import com.wo.common.result.PageResult;
import com.wo.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class WorkOrderClientFallback implements FallbackFactory<WorkOrderClient> {

    @Override
    public WorkOrderClient create(Throwable cause) {
        log.error("WorkOrderClient fallback triggered", cause);
        return new WorkOrderClient() {

            @Override
            public R<WorkOrderVO> getWorkOrder(Long id) {
                return R.fail("工单服务不可用，请稍后重试");
            }

            @Override
            public R<PageResult<WorkOrderBriefVO>> queryWorkOrders(WorkOrderQueryDTO query) {
                return R.fail("工单服务不可用，请稍后重试");
            }

            @Override
            public R<WorkOrderVO> createWorkOrder(WorkOrderCreateDTO dto) {
                return R.fail("工单服务不可用，请稍后重试");
            }

            @Override
            public R<Void> updateWorkOrderStatus(Long id, WorkOrderStatusUpdateDTO dto) {
                return R.fail("工单服务不可用，请稍后重试");
            }

            @Override
            public R<Void> assignWorkOrder(Long id, WorkOrderAssignDTO dto) {
                return R.fail("工单服务不可用，请稍后重试");
            }

            @Override
            public R<Void> rejectResolution(Long id, WorkOrderRejectDTO dto) {
                return R.fail("work order service unavailable");
            }

            @Override
            public R<List<FlowRecordVO>> listFlowRecords(Long id) {
                return R.fail("work order service unavailable");
            }

            @Override
            public R<List<CommentVO>> getComments(Long workOrderId) {
                return R.fail("工单服务不可用，请稍后重试");
            }

            @Override
            public R<CommentVO> addComment(Long workOrderId, CommentCreateDTO dto) {
                return R.fail("工单服务不可用，请稍后重试");
            }

            @Override
            public R<Void> updateSlaDeadline(Long id, String deadline) {
                return R.fail("工单服务不可用，请稍后重试");
            }

            @Override
            public R<List<Long>> listSlaBreachedWorkOrderIds(String deadlineBefore) {
                return R.fail("工单服务不可用，请稍后重试");
            }

            @Override
            public R<Void> markSlaBreached(Long id) {
                return R.fail("工单服务不可用，请稍后重试");
            }
        };
    }
}
