package com.wo.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wo.workorder.entity.WoFlowRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FlowRecordMapper extends BaseMapper<WoFlowRecord> {
}
