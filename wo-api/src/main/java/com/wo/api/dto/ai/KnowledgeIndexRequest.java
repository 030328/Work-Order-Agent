package com.wo.api.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeIndexRequest implements Serializable {

    private String title;

    @NotBlank(message = "知识内容不能为空")
    private String content;

    private String sourceType;

    private String sourceId;

    private String category;

    private Integer verified;

    private Integer likeCount;
}
