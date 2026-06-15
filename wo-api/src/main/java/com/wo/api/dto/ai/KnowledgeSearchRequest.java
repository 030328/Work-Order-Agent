package com.wo.api.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class KnowledgeSearchRequest implements Serializable {

    @NotBlank(message = "搜索关键词不能为空")
    private String query;

    private Integer topK = 5;

    private String category;
}
