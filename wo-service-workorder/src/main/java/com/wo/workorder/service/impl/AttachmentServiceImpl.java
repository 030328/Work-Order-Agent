package com.wo.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wo.api.client.UserClient;
import com.wo.api.dto.user.UserInfo;
import com.wo.api.dto.workorder.AttachmentCreateDTO;
import com.wo.api.dto.workorder.AttachmentVO;
import com.wo.common.enums.ErrorCode;
import com.wo.common.exception.BizException;
import com.wo.common.result.R;
import com.wo.workorder.entity.WoAttachment;
import com.wo.workorder.entity.WoFlowRecord;
import com.wo.workorder.mapper.AttachmentMapper;
import com.wo.workorder.mapper.FlowRecordMapper;
import com.wo.workorder.service.AttachmentService;
import com.wo.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentMapper attachmentMapper;
    private final FlowRecordMapper flowRecordMapper;
    private final WorkOrderService workOrderService;
    private final UserClient userClient;

    @Override
    public List<AttachmentVO> listAttachments(Long workOrderId) {
        workOrderService.getWorkOrder(workOrderId);
        LambdaQueryWrapper<WoAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WoAttachment::getWorkOrderId, workOrderId)
                .orderByDesc(WoAttachment::getCreatedAt);
        return attachmentMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public AttachmentVO createAttachment(Long workOrderId, AttachmentCreateDTO dto, Long uploaderId) {
        workOrderService.getWorkOrder(workOrderId);
        if (uploaderId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        WoAttachment attachment = new WoAttachment();
        attachment.setWorkOrderId(workOrderId);
        attachment.setFileName(dto.getFileName());
        attachment.setFileUrl(dto.getFileUrl());
        attachment.setFileSize(dto.getFileSize());
        attachment.setFileType(dto.getFileType());
        attachment.setUploaderId(uploaderId);
        attachmentMapper.insert(attachment);
        saveFlowRecord(workOrderId, uploaderId, "Attachment: " + dto.getFileName());
        return convertToVO(attachment);
    }

    @Override
    public AttachmentVO uploadAttachment(Long workOrderId, MultipartFile file, Long uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "file is required");
        }
        String fileName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "attachment";
        AttachmentCreateDTO dto = new AttachmentCreateDTO();
        dto.setFileName(fileName);
        dto.setFileSize(file.getSize());
        dto.setFileType(file.getContentType());
        dto.setFileUrl("metadata-only://workorders/" + workOrderId + "/attachments/" + fileName.replace('\\', '_').replace('/', '_'));
        return createAttachment(workOrderId, dto, uploaderId);
    }

    private AttachmentVO convertToVO(WoAttachment attachment) {
        AttachmentVO vo = new AttachmentVO();
        BeanUtils.copyProperties(attachment, vo);
        vo.setUploaderName(resolveUserName(attachment.getUploaderId()));
        return vo;
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            R<UserInfo> resp = userClient.getUserInfo(userId);
            if (resp != null && resp.getData() != null) {
                return resp.getData().getRealName();
            }
        } catch (Exception e) {
            log.warn("Resolve uploader name failed, userId={}", userId, e);
        }
        return "User#" + userId;
    }

    private void saveFlowRecord(Long workOrderId, Long uploaderId, String comment) {
        WoFlowRecord record = new WoFlowRecord();
        record.setWorkOrderId(workOrderId);
        record.setAction("ATTACHMENT");
        record.setOperatorId(uploaderId);
        record.setComment(comment);
        record.setIsSystem(0);
        flowRecordMapper.insert(record);
    }
}
