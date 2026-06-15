package com.wo.workorder.controller;

import com.wo.api.dto.workorder.CommentCreateDTO;
import com.wo.api.dto.workorder.CommentVO;
import com.wo.common.result.R;
import com.wo.common.util.SecurityUtil;
import com.wo.workorder.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workorders/{workOrderId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 获取工单评论列表
     */
    @GetMapping
    public R<List<CommentVO>> getComments(@PathVariable Long workOrderId) {
        List<CommentVO> comments = commentService.getComments(workOrderId);
        return R.ok(comments);
    }

    /**
     * 添加评论
     */
    @PostMapping
    public R<Void> addComment(@PathVariable Long workOrderId,
                              @Valid @RequestBody CommentCreateDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        commentService.addComment(workOrderId, dto, userId, false);
        return R.ok();
    }
}
