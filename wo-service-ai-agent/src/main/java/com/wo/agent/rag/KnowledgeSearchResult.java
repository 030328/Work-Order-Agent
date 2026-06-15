package com.wo.agent.rag;

import lombok.Data;

/**
 * 知识库搜索结果
 */
@Data
public class KnowledgeSearchResult {

    /**
     * 结果ID
     */
    private String id;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 内容片段
     */
    private String content;

    /**
     * 来源ID
     */
    private String sourceId;

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 相似度分数 (0-1)
     */
    private float score;
}
