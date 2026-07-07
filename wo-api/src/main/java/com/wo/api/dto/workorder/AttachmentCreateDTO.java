package com.wo.api.dto.workorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class AttachmentCreateDTO implements Serializable {

    @NotBlank(message = "fileName is required")
    @Size(max = 200, message = "fileName must not exceed 200 characters")
    private String fileName;

    @NotBlank(message = "fileUrl is required")
    @Size(max = 500, message = "fileUrl must not exceed 500 characters")
    private String fileUrl;

    @PositiveOrZero(message = "fileSize must not be negative")
    private Long fileSize;

    @Size(max = 50, message = "fileType must not exceed 50 characters")
    private String fileType;
}
