package com.wo.api.dto.workorder;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AttachmentVO implements Serializable {

    private Long id;

    private Long workOrderId;

    private String fileName;

    private String fileUrl;

    private Long fileSize;

    private String fileType;

    private Long uploaderId;

    private String uploaderName;

    private LocalDateTime createdAt;
}
