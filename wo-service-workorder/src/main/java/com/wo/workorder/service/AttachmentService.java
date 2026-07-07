package com.wo.workorder.service;

import com.wo.api.dto.workorder.AttachmentCreateDTO;
import com.wo.api.dto.workorder.AttachmentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {

    List<AttachmentVO> listAttachments(Long workOrderId);

    AttachmentVO createAttachment(Long workOrderId, AttachmentCreateDTO dto, Long uploaderId);

    AttachmentVO uploadAttachment(Long workOrderId, MultipartFile file, Long uploaderId);
}
