package com.wo.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wo.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wo_attachment")
public class WoAttachment extends BaseEntity {

    /**
     * 工单ID
     */
    private Long workOrderId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 上传人ID
     */
    private Long uploaderId;
}
