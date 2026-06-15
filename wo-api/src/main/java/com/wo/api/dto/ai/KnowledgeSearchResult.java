package com.wo.api.dto.ai;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class KnowledgeSearchResult implements Serializable {

    private String id;

    private String title;

    private String content;

    private String sourceType;

    private Double score;

    private Map<String, Object> metadata;
}
