package com.wo.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wo.workorder.entity.WoComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<WoComment> {
}
