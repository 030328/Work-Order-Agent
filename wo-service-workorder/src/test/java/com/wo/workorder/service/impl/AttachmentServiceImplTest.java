package com.wo.workorder.service.impl;

import com.wo.api.client.UserClient;
import com.wo.api.dto.user.UserInfo;
import com.wo.api.dto.workorder.AttachmentCreateDTO;
import com.wo.api.dto.workorder.AttachmentVO;
import com.wo.api.dto.workorder.WorkOrderVO;
import com.wo.common.result.R;
import com.wo.workorder.entity.WoAttachment;
import com.wo.workorder.entity.WoFlowRecord;
import com.wo.workorder.mapper.AttachmentMapper;
import com.wo.workorder.mapper.FlowRecordMapper;
import com.wo.workorder.service.WorkOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttachmentServiceImplTest {

    @Test
    void createAttachmentShouldValidateWorkOrderAndPersistMetadata() {
        AttachmentMapper attachmentMapper = mock(AttachmentMapper.class);
        FlowRecordMapper flowRecordMapper = mock(FlowRecordMapper.class);
        WorkOrderService workOrderService = mock(WorkOrderService.class);
        UserClient userClient = mock(UserClient.class);

        WorkOrderVO workOrder = new WorkOrderVO();
        workOrder.setId(1001L);
        when(workOrderService.getWorkOrder(1001L)).thenReturn(workOrder);
        when(attachmentMapper.insert(any(WoAttachment.class))).thenReturn(1);
        when(flowRecordMapper.insert(any(WoFlowRecord.class))).thenReturn(1);

        UserInfo uploader = new UserInfo();
        uploader.setId(20L);
        uploader.setRealName("Agent");
        when(userClient.getUserInfo(20L)).thenReturn(R.ok(uploader));

        AttachmentServiceImpl service = new AttachmentServiceImpl(attachmentMapper, flowRecordMapper, workOrderService, userClient);

        AttachmentCreateDTO dto = new AttachmentCreateDTO();
        dto.setFileName("error-log.txt");
        dto.setFileUrl("https://example.com/error-log.txt");
        dto.setFileSize(1024L);
        dto.setFileType("text/plain");

        AttachmentVO result = service.createAttachment(1001L, dto, 20L);

        assertThat(result.getFileName()).isEqualTo("error-log.txt");
        assertThat(result.getUploaderName()).isEqualTo("Agent");

        ArgumentCaptor<WoAttachment> attachmentCaptor = ArgumentCaptor.forClass(WoAttachment.class);
        verify(attachmentMapper).insert(attachmentCaptor.capture());
        assertThat(attachmentCaptor.getValue().getWorkOrderId()).isEqualTo(1001L);
        assertThat(attachmentCaptor.getValue().getUploaderId()).isEqualTo(20L);

        ArgumentCaptor<WoFlowRecord> flowCaptor = ArgumentCaptor.forClass(WoFlowRecord.class);
        verify(flowRecordMapper).insert(flowCaptor.capture());
        assertThat(flowCaptor.getValue().getAction()).isEqualTo("ATTACHMENT");
        assertThat(flowCaptor.getValue().getComment()).contains("error-log.txt");
    }
}
