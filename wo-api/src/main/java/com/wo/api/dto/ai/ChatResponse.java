package com.wo.api.dto.ai;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ChatResponse implements Serializable {

    private String sessionId;

    private String content;

    private List<ToolCall> toolCalls;

    private String role;

    private boolean finished;

    @Data
    public static class ToolCall implements Serializable {

        private String id;

        private String name;

        private String arguments;
    }
}
