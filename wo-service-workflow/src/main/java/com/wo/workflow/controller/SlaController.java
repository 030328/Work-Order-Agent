package com.wo.workflow.controller;

import com.wo.common.result.R;
import com.wo.workflow.service.SlaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflow/sla")
@RequiredArgsConstructor
public class SlaController {

    private final SlaService slaService;

    @PostMapping("/assign")
    public R<Void> assignSla(@RequestParam Long workOrderId, @RequestParam String priority) {
        slaService.assignSla(workOrderId, priority);
        return R.ok();
    }

    @GetMapping("/deadline")
    public R<String> calculateSlaDeadline(@RequestParam String priority) {
        var deadline = slaService.calculateResolveDeadline(priority);
        return deadline != null ? R.ok(deadline.toString()) : R.fail("未找到匹配的SLA规则");
    }

    @PostMapping("/check")
    public R<Void> checkSlaBreaches() {
        slaService.checkSlaBreaches();
        return R.ok();
    }
}
