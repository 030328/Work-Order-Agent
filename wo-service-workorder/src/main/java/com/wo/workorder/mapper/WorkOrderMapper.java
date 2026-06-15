package com.wo.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wo.workorder.entity.WoWorkOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkOrderMapper extends BaseMapper<WoWorkOrder> {
}
