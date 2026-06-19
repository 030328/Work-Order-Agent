package com.wo.api.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse implements Serializable {

    private String sessionId;

    private String content;

    private List<ToolCall> toolCalls;

    private String role;

    private boolean finished;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall implements Serializable {

        private String id;

        private String name;

        private String arguments;
    }
}
