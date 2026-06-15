package com.wo.workorder.controller;

import com.wo.common.result.R;
import com.wo.workorder.entity.WoAttachment;
import com.wo.workorder.mapper.AttachmentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/workorders/{workOrderId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentMapper attachmentMapper;

    /**
     * 上传附件（占位实现）
     */
    @PostMapping
    public R<WoAttachment> upload(@PathVariable Long workOrderId,
                                  @RequestParam("file") MultipartFile file) {
        log.info("收到附件上传请求, workOrderId={}, fileName={}", workOrderId, file.getOriginalFilename());

        // TODO: 实现文件上传到OSS/MinIO
        WoAttachment attachment = new WoAttachment();
        attachment.setWorkOrderId(workOrderId);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        attachment.setFileUrl("placeholder-url");

        attachmentMapper.insert(attachment);
        return R.ok(attachment);
    }

    /**
     * 获取工单附件列表
     */
    @GetMapping
    public R<List<WoAttachment>> list(@PathVariable Long workOrderId) {
        LambdaQueryWrapper<WoAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WoAttachment::getWorkOrderId, workOrderId)
                .orderByDesc(WoAttachment::getCreatedAt);
        List<WoAttachment> attachments = attachmentMapper.selectList(wrapper);
        return R.ok(attachments);
    }
}
