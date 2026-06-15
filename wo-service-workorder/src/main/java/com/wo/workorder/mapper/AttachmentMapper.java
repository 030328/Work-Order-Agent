package com.wo.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wo.workorder.entity.WoAttachment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttachmentMapper extends BaseMapper<WoAttachment> {
}
