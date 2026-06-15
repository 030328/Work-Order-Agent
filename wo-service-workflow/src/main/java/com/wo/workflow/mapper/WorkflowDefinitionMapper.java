package com.wo.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wo.workflow.entity.WfDefinition;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowDefinitionMapper extends BaseMapper<WfDefinition> {
}
