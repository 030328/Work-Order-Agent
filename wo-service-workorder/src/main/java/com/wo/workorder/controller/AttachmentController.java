package com.wo.workorder.controller;

import com.wo.api.dto.workorder.AttachmentCreateDTO;
import com.wo.api.dto.workorder.AttachmentVO;
import com.wo.common.result.R;
import com.wo.common.util.SecurityUtil;
import com.wo.workorder.service.AttachmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/workorders/{workOrderId}/attachments")
@RequiredArgsConstructor
@Validated
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping
    public R<AttachmentVO> upload(@PathVariable @Positive Long workOrderId,
                                  @RequestParam("file") MultipartFile file) {
        Long uploaderId = SecurityUtil.requireCurrentUserId();
        return R.ok(attachmentService.uploadAttachment(workOrderId, file, uploaderId));
    }

    @PostMapping("/metadata")
    public R<AttachmentVO> createMetadata(@PathVariable @Positive Long workOrderId,
                                          @Valid @RequestBody AttachmentCreateDTO dto) {
        Long uploaderId = SecurityUtil.requireCurrentUserId();
        return R.ok(attachmentService.createAttachment(workOrderId, dto, uploaderId));
    }

    @GetMapping
    public R<List<AttachmentVO>> list(@PathVariable @Positive Long workOrderId) {
        return R.ok(attachmentService.listAttachments(workOrderId));
    }
}
