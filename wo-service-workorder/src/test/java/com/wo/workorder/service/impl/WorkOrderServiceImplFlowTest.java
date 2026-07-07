package com.wo.workorder.service.impl;

import com.wo.api.client.AiAgentClient;
import com.wo.api.client.UserClient;
import com.wo.api.client.WorkflowClient;
import com.wo.api.dto.ai.KnowledgeIndexRequest;
import com.wo.api.dto.ai.WorkOrderAnalysisResult;
import com.wo.api.dto.user.UserInfo;
import com.wo.api.dto.workorder.WorkOrderCreateDTO;
import com.wo.api.dto.workorder.WorkOrderVO;
import com.wo.api.dto.workflow.TransitionRequest;
import com.wo.api.dto.workflow.TransitionResult;
import com.wo.common.enums.WorkOrderStatus;
import com.wo.common.result.PageResult;
import com.wo.common.result.R;
import com.wo.workorder.entity.WoFlowRecord;
import com.wo.workorder.entity.WoWorkOrder;
import com.wo.workorder.mapper.FlowRecordMapper;
import com.wo.workorder.mapper.WorkOrderMapper;
import com.wo.workorder.service.WorkOrderEventPublisher;
import com.wo.workorder.service.WorkOrderSearchService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkOrderServiceImplFlowTest {

    @Test
    void aiFirstManualFlowCanBeRejectedAndClosed() {
        WorkOrderMapper workOrderMapper = mock(WorkOrderMapper.class);
        FlowRecordMapper flowRecordMapper = mock(FlowRecordMapper.class);
        UserClient userClient = mock(UserClient.class);
        AiAgentClient aiAgentClient = mock(AiAgentClient.class);
        WorkflowClient workflowClient = mock(WorkflowClient.class);
        WorkOrderSearchService workOrderSearchService = mock(WorkOrderSearchService.class);
        WorkOrderEventPublisher workOrderEventPublisher = mock(WorkOrderEventPublisher.class);

        Map<Long, WoWorkOrder> store = new HashMap<>();
        AtomicLong idSequence = new AtomicLong(10001L);
        when(workOrderMapper.insert(any(WoWorkOrder.class))).thenAnswer(invocation -> {
            WoWorkOrder workOrder = invocation.getArgument(0);
            workOrder.setId(idSequence.getAndIncrement());
            workOrder.setCreatedAt(LocalDateTime.now());
            workOrder.setUpdatedAt(LocalDateTime.now());
            store.put(workOrder.getId(), workOrder);
            return 1;
        });
        when(workOrderMapper.updateById(any(WoWorkOrder.class))).thenAnswer(invocation -> {
            WoWorkOrder workOrder = invocation.getArgument(0);
            workOrder.setUpdatedAt(LocalDateTime.now());
            store.put(workOrder.getId(), workOrder);
            return 1;
        });
        when(workOrderMapper.selectById(any())).thenAnswer(invocation -> store.get(invocation.getArgument(0)));
        when(flowRecordMapper.insert(any(WoFlowRecord.class))).thenReturn(1);

        UserInfo creator = new UserInfo();
        creator.setId(100L);
        creator.setRealName("Submitter");
        when(userClient.getUserInfo(anyLong())).thenReturn(R.ok(creator));

        when(workflowClient.calculateSlaDeadline(anyString()))
                .thenReturn(R.ok(LocalDateTime.now().plusHours(24).toString()));
        when(workflowClient.executeTransition(any(TransitionRequest.class))).thenAnswer(invocation -> {
            TransitionRequest request = invocation.getArgument(0);
            return R.ok(TransitionResult.success(request.getFromStatus(), request.getToStatus()));
        });

        when(workOrderSearchService.search(anyString(), anyInt(), anyInt()))
                .thenReturn(PageResult.of(0, 1, 5, List.of()));
        when(aiAgentClient.analyzeWorkOrder(any())).thenReturn(R.ok(WorkOrderAnalysisResult.builder()
                .suggestedCategory("BUG")
                .suggestedPriority("HIGH")
                .summary("Login failure")
                .sentiment("NEGATIVE")
                .suggestedSolution("Check gateway route and backend health")
                .build()));
        when(aiAgentClient.indexKnowledge(any(KnowledgeIndexRequest.class))).thenReturn(R.ok("indexed"));

        WorkOrderServiceImpl service = new WorkOrderServiceImpl(
                workOrderMapper,
                flowRecordMapper,
                userClient,
                aiAgentClient,
                workflowClient,
                workOrderSearchService,
                workOrderEventPublisher
        );

        WorkOrderCreateDTO dto = new WorkOrderCreateDTO();
        dto.setTitle("Login failure");
        dto.setDescription("User sees a blank page after login");
        dto.setCategory("BUG");
        dto.setPriority("HIGH");

        WorkOrderVO created = service.createWorkOrder(dto, 100L);
        Long workOrderId = created.getId();
        assertThat(created.getStatus()).isEqualTo(WorkOrderStatus.AI_SOLVED.getCode());
        assertThat(created.getAiSuggestedSolution()).contains("gateway route");

        service.escalateWorkOrder(workOrderId, 100L);
        assertThat(store.get(workOrderId).getStatus()).isEqualTo(WorkOrderStatus.ESCALATED.getCode());

        service.claimWorkOrder(workOrderId, 200L);
        assertThat(store.get(workOrderId).getStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS.getCode());
        assertThat(store.get(workOrderId).getAssigneeId()).isEqualTo(200L);

        service.updateStatus(workOrderId, WorkOrderStatus.RESOLVED.getCode(), 200L, "gateway config fixed");
        assertThat(store.get(workOrderId).getStatus()).isEqualTo(WorkOrderStatus.RESOLVED.getCode());
        assertThat(store.get(workOrderId).getResolution()).isEqualTo("gateway config fixed");

        service.rejectResolution(workOrderId, 100L, "still cannot login");
        assertThat(store.get(workOrderId).getStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS.getCode());
        assertThat(store.get(workOrderId).getResolvedAt()).isNull();

        service.updateStatus(workOrderId, WorkOrderStatus.RESOLVED.getCode(), 200L, "login callback config fixed");
        assertThat(store.get(workOrderId).getStatus()).isEqualTo(WorkOrderStatus.RESOLVED.getCode());

        service.confirmWorkOrder(workOrderId, 100L);
        assertThat(store.get(workOrderId).getStatus()).isEqualTo(WorkOrderStatus.CLOSED.getCode());
        assertThat(store.get(workOrderId).getClosedAt()).isNotNull();

        verify(workOrderSearchService, never()).indexWorkOrder(any(WoWorkOrder.class));
        verify(workOrderEventPublisher, atLeast(8)).publishStatusChangeEvent(any(), any(), any());
        verify(workOrderEventPublisher).publishCloseEvent(workOrderId);

        ArgumentCaptor<KnowledgeIndexRequest> knowledgeCaptor = ArgumentCaptor.forClass(KnowledgeIndexRequest.class);
        verify(aiAgentClient).indexKnowledge(knowledgeCaptor.capture());
        assertThat(knowledgeCaptor.getValue().getSourceType()).isEqualTo("HISTORICAL_WO");
        assertThat(knowledgeCaptor.getValue().getContent()).contains("login callback config fixed");

        ArgumentCaptor<WoFlowRecord> recordCaptor = ArgumentCaptor.forClass(WoFlowRecord.class);
        verify(flowRecordMapper, atLeast(8)).insert(recordCaptor.capture());
        assertThat(recordCaptor.getAllValues())
                .extracting(WoFlowRecord::getAction)
                .contains("CREATE", "AI_SOLVED", "ESCALATE", "CLAIM", "STATUS_CHANGE", "REJECT_RESOLUTION", "CONFIRM");
    }
}
