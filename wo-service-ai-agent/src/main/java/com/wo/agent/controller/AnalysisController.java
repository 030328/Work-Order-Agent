package com.wo.agent.controller;

import com.wo.agent.service.WorkOrderAnalysisService;
import com.wo.api.dto.ai.WorkOrderAnalysisRequest;
import com.wo.api.dto.ai.WorkOrderAnalysisResult;
import com.wo.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AnalysisController {

    private final WorkOrderAnalysisService analysisService;

    @PostMapping("/analyze")
    public R<WorkOrderAnalysisResult> analyzeWorkOrder(@Valid @RequestBody WorkOrderAnalysisRequest request) {
        WorkOrderAnalysisResult result = analysisService.analyze(request);
        return R.ok(result);
    }
}
