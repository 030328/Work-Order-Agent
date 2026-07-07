package com.wo.workorder.service;

import com.wo.api.dto.workorder.CommentCreateDTO;
import com.wo.api.dto.workorder.CommentVO;

import java.util.List;

public interface CommentService {

    /**
     * 获取工单评论列表
     */
    List<CommentVO> getComments(Long workOrderId);

    /**
     * 添加评论
     */
    CommentVO addComment(Long workOrderId, CommentCreateDTO dto, Long userId, boolean isAiGenerated);
}
