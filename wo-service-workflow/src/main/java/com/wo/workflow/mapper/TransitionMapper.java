package com.wo.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wo.workflow.entity.WfTransition;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransitionMapper extends BaseMapper<WfTransition> {
}
