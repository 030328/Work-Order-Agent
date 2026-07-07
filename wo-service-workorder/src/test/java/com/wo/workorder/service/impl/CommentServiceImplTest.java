package com.wo.workorder.service.impl;

import com.wo.api.client.UserClient;
import com.wo.api.dto.user.UserInfo;
import com.wo.api.dto.workorder.CommentCreateDTO;
import com.wo.api.dto.workorder.CommentVO;
import com.wo.common.result.R;
import com.wo.workorder.entity.WoComment;
import com.wo.workorder.entity.WoFlowRecord;
import com.wo.workorder.entity.WoWorkOrder;
import com.wo.workorder.mapper.CommentMapper;
import com.wo.workorder.mapper.FlowRecordMapper;
import com.wo.workorder.mapper.WorkOrderMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceImplTest {

    @Test
    void addCommentShouldPersistCommentAndFlowRecord() {
        CommentMapper commentMapper = mock(CommentMapper.class);
        WorkOrderMapper workOrderMapper = mock(WorkOrderMapper.class);
        FlowRecordMapper flowRecordMapper = mock(FlowRecordMapper.class);
        UserClient userClient = mock(UserClient.class);

        WoWorkOrder workOrder = new WoWorkOrder();
        workOrder.setId(1001L);
        workOrder.setStatus("IN_PROGRESS");
        workOrder.setCreatorId(10L);
        workOrder.setAssigneeId(20L);
        when(workOrderMapper.selectById(1001L)).thenReturn(workOrder);
        when(commentMapper.insert(any(WoComment.class))).thenReturn(1);
        when(flowRecordMapper.insert(any(WoFlowRecord.class))).thenReturn(1);

        UserInfo user = new UserInfo();
        user.setId(20L);
        user.setRealName("Agent");
        when(userClient.getUserInfo(20L)).thenReturn(R.ok(user));

        CommentServiceImpl service = new CommentServiceImpl(commentMapper, workOrderMapper, flowRecordMapper, userClient);

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setContent("Checked gateway logs");
        dto.setIsInternal(false);

        CommentVO result = service.addComment(1001L, dto, 20L, false);

        assertThat(result.getContent()).isEqualTo("Checked gateway logs");
        assertThat(result.getUserName()).isEqualTo("Agent");
        assertThat(result.getIsInternal()).isFalse();

        ArgumentCaptor<WoComment> commentCaptor = ArgumentCaptor.forClass(WoComment.class);
        verify(commentMapper).insert(commentCaptor.capture());
        assertThat(commentCaptor.getValue().getWorkOrderId()).isEqualTo(1001L);
        assertThat(commentCaptor.getValue().getUserId()).isEqualTo(20L);

        ArgumentCaptor<WoFlowRecord> flowCaptor = ArgumentCaptor.forClass(WoFlowRecord.class);
        verify(flowRecordMapper).insert(flowCaptor.capture());
        assertThat(flowCaptor.getValue().getAction()).isEqualTo("COMMENT");
        assertThat(flowCaptor.getValue().getFromStatus()).isEqualTo("IN_PROGRESS");
        assertThat(flowCaptor.getValue().getToStatus()).isEqualTo("IN_PROGRESS");
    }
}
