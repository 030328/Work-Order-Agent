package com.wo.api.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class ChatRequest implements Serializable {

    private String sessionId;

    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * Nullable. Bind the chat session to a specific work order for context.
     */
    private Long workOrderId;
}
