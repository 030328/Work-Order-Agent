package com.wo.api.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeSearchResult implements Serializable {

    private Long id;

    private String title;

    private String content;

    private String sourceType;

    private String sourceId;

    private String category;

    private Double score;

    private Integer verified;

    private Integer likeCount;
}
