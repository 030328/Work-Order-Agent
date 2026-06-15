package com.wo.api.dto.workorder;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class WorkOrderUpdateDTO implements Serializable {

    @Size(max = 200, message = "工单标题长度不能超过200个字符")
    private String title;

    @Size(max = 5000, message = "工单描述长度不能超过5000个字符")
    private String description;

    private String category;

    private String priority;

    private List<String> tags;
}
