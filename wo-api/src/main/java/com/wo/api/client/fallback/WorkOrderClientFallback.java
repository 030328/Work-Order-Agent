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
            public R<Void> updateWorkOrderStatus(Long id, String status, String comment) {
                return R.fail("工单服务不可用，请稍后重试");
            }

            @Override
            public R<Void> assignWorkOrder(Long id, Long assigneeId, String reason) {
                return R.fail("工单服务不可用，请稍后重试");
            }

            @Override
            public R<List<CommentVO>> getComments(Long workOrderId) {
                return R.fail("工单服务不可用，请稍后重试");
            }

            @Override
            public R<Void> addComment(Long workOrderId, CommentCreateDTO dto) {
                return R.fail("工单服务不可用，请稍后重试");
            }
        };
    }
}
