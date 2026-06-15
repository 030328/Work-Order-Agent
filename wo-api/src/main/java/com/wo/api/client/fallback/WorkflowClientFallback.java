package com.wo.api.client.fallback;

import com.wo.api.client.WorkflowClient;
import com.wo.api.dto.workflow.TransitionRequest;
import com.wo.api.dto.workflow.TransitionResult;
import com.wo.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WorkflowClientFallback implements FallbackFactory<WorkflowClient> {

    @Override
    public WorkflowClient create(Throwable cause) {
        log.error("WorkflowClient fallback triggered", cause);
        return new WorkflowClient() {

            @Override
            public R<TransitionResult> executeTransition(TransitionRequest request) {
                return R.fail("工作流服务不可用，请稍后重试");
            }

            @Override
            public R<Void> validateTransition(Long workOrderId, String targetStatus) {
                return R.fail("工作流服务不可用，请稍后重试");
            }
        };
    }
}
