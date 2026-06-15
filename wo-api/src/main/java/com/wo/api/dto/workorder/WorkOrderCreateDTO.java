package com.wo.api.dto.workorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class WorkOrderCreateDTO implements Serializable {

    @NotBlank(message = "工单标题不能为空")
    @Size(max = 200, message = "工单标题长度不能超过200个字符")
    private String title;

    @NotBlank(message = "工单描述不能为空")
    @Size(max = 5000, message = "工单描述长度不能超过5000个字符")
    private String description;

    @NotBlank(message = "工单分类不能为空")
    private String category;

    @NotBlank(message = "优先级不能为空")
    private String priority;

    private Long assigneeId;

    private List<String> tags;
}
