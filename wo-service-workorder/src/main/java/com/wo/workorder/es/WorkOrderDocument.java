package com.wo.workorder.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "wo_work_order")
public class WorkOrderDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String orderNo;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String priority;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Long)
    private Long creatorId;

    @Field(type = FieldType.Long)
    private Long assigneeId;

    @Field(type = FieldType.Keyword)
    private String tags;

    @Field(type = FieldType.Text)
    private String resolution;

    @Field(type = FieldType.Date)
    private String createdAt;
}
