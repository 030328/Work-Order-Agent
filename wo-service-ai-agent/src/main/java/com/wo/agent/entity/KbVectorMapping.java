package com.wo.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wo.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库向量映射实体
 * 记录文档分块与Milvus向量的对应关系
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_vector_mapping")
public class KbVectorMapping extends BaseEntity {

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 分块索引
     */
    private Integer chunkIndex;

    /**
     * 分块文本内容（长文本）
     */
    private String chunkText;

    /**
     * Milvus中的向量ID
     */
    private String milvusId;

    /**
     * Token数量
     */
    private Integer tokenCount;
}
