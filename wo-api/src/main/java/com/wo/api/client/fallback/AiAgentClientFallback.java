package com.wo.api.client.fallback;

import com.wo.api.client.AiAgentClient;
import com.wo.api.dto.ai.WorkOrderAnalysisRequest;
import com.wo.api.dto.ai.WorkOrderAnalysisResult;
import com.wo.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AiAgentClientFallback implements FallbackFactory<AiAgentClient> {

    @Override
    public AiAgentClient create(Throwable cause) {
        log.error("AI Agent service fallback", cause);
        return new AiAgentClient() {
            @Override
            public R<WorkOrderAnalysisResult> analyzeWorkOrder(WorkOrderAnalysisRequest request) {
                return R.fail("AI分析服务暂时不可用");
            }
        };
    }
}
