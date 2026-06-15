package com.wo.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wo.api.client.UserClient;
import com.wo.api.dto.user.UserInfo;
import com.wo.api.dto.workorder.CommentCreateDTO;
import com.wo.api.dto.workorder.CommentVO;
import com.wo.common.result.R;
import com.wo.workorder.entity.WoComment;
import com.wo.workorder.mapper.CommentMapper;
import com.wo.workorder.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final UserClient userClient;

    @Override
    public List<CommentVO> getComments(Long workOrderId) {
        LambdaQueryWrapper<WoComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WoComment::getWorkOrderId, workOrderId)
                .orderByAsc(WoComment::getCreatedAt);

        List<WoComment> comments = commentMapper.selectList(wrapper);

        return comments.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public void addComment(Long workOrderId, CommentCreateDTO dto, Long userId, boolean isAiGenerated) {
        WoComment comment = new WoComment();
        comment.setWorkOrderId(workOrderId);
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setIsInternal(Boolean.TRUE.equals(dto.getIsInternal()) ? 1 : 0);
        comment.setIsAiGenerated(isAiGenerated ? 1 : 0);

        commentMapper.insert(comment);
        log.info("评论添加成功, workOrderId={}, userId={}, isAi={}", workOrderId, userId, isAiGenerated);
    }

    private CommentVO convertToVO(WoComment comment) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);

        // 转换Boolean字段
        vo.setIsInternal(comment.getIsInternal() != null && comment.getIsInternal() == 1);
        vo.setIsAiGenerated(comment.getIsAiGenerated() != null && comment.getIsAiGenerated() == 1);

        // 获取用户名
        if (comment.getUserId() != null) {
            try {
                R<UserInfo> resp = userClient.getUserInfo(comment.getUserId());
                if (resp != null && resp.getData() != null) {
                    vo.setUserName(resp.getData().getRealName());
                }
            } catch (Exception e) {
                log.warn("获取用户信息失败, userId={}", comment.getUserId(), e);
            }
        }

        return vo;
    }
}
