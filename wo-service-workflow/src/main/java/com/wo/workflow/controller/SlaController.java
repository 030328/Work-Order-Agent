package com.wo.workflow.controller;

import com.wo.common.result.R;
import com.wo.common.security.InternalServiceAuth;
import com.wo.workflow.service.SlaService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final InternalServiceAuth internalServiceAuth;

    @PostMapping("/assign")
    public R<Void> assignSla(@RequestParam Long workOrderId,
                             @RequestParam String priority,
                             HttpServletRequest request) {
        internalServiceAuth.require(request);
        slaService.assignSla(workOrderId, priority);
        return R.ok();
    }

    @GetMapping("/deadline")
    public R<String> calculateSlaDeadline(@RequestParam String priority,
                                          HttpServletRequest request) {
        internalServiceAuth.require(request);
        var deadline = slaService.calculateResolveDeadline(priority);
        return deadline != null ? R.ok(deadline.toString()) : R.fail("未找到匹配的SLA规则");
    }

    @PostMapping("/check")
    public R<Void> checkSlaBreaches(HttpServletRequest request) {
        internalServiceAuth.require(request);
        slaService.checkSlaBreaches();
        return R.ok();
    }
}
