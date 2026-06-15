package com.wo.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wo.workflow.entity.WfSlaRule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SlaRuleMapper extends BaseMapper<WfSlaRule> {
}
