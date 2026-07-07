package com.wo.agent.tool;

import com.wo.agent.rag.RagService;
import com.wo.api.client.WorkOrderClient;
import com.wo.api.client.UserClient;
import com.wo.api.dto.ai.KnowledgeSearchResult;
import com.wo.api.dto.workorder.*;
import com.wo.api.dto.user.UserVO;
import com.wo.common.result.PageResult;
import com.wo.common.result.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工单系统工具集 - 供 AI Agent Tool Calling 使用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderTools {

    private final WorkOrderClient workOrderClient;
    private final UserClient userClient;
    private final RagService ragService;

    /**
     * 创建工单
     */
    public String createWorkOrder(String title, String description, String category, String priority) {
        log.info("Tool: createWorkOrder - title={}, category={}, priority={}", title, category, priority);
        try {
            WorkOrderCreateDTO dto = new WorkOrderCreateDTO();
            dto.setTitle(title);
            dto.setDescription(description);
            dto.setCategory(category);
            dto.setPriority(priority);

            R<WorkOrderVO> result = workOrderClient.createWorkOrder(dto);
            if (result != null && result.getData() != null) {
                WorkOrderVO vo = result.getData();
                return String.format("工单创建成功！\n工单编号: %s\n标题: %s\n状态: %s\nAI分析摘要: %s",
                        vo.getOrderNo(), vo.getTitle(), vo.getStatus(), vo.getAiSummary());
            }
            return "工单创建失败：服务返回空结果";
        } catch (Exception e) {
            log.error("Tool createWorkOrder failed", e);
            return "工单创建失败：" + e.getMessage();
        }
    }

    /**
     * 查询工单列表
     */
    public String searchWorkOrders(String keyword, String status, String priority) {
        log.info("Tool: searchWorkOrders - keyword={}, status={}, priority={}", keyword, status, priority);
        try {
            WorkOrderQueryDTO query = new WorkOrderQueryDTO();
            query.setKeyword(keyword);
            query.setStatus(status);
            query.setPriority(priority);
            query.setPage(1);
            query.setSize(5);

            R<PageResult<WorkOrderBriefVO>> result = workOrderClient.queryWorkOrders(query);
            if (result != null && result.getData() != null) {
                List<WorkOrderBriefVO> orders = result.getData().getRecords();
                if (orders.isEmpty()) {
                    return "未找到匹配的工单";
                }
                StringBuilder sb = new StringBuilder("找到 " + result.getData().getTotal() + " 个工单：\n");
                for (int i = 0; i < orders.size(); i++) {
                    WorkOrderBriefVO vo = orders.get(i);
                    sb.append(String.format("%d. [%s] %s (状态: %s, 优先级: %s)\n",
                            i + 1, vo.getOrderNo(), vo.getTitle(), vo.getStatus(), vo.getPriority()));
                }
                return sb.toString();
            }
            return "查询失败：服务返回空结果";
        } catch (Exception e) {
            log.error("Tool searchWorkOrders failed", e);
            return "查询失败：" + e.getMessage();
        }
    }

    /**
     * 获取工单详情
     */
    public String getWorkOrderDetail(Long workOrderId) {
        log.info("Tool: getWorkOrderDetail - id={}", workOrderId);
        try {
            R<WorkOrderVO> result = workOrderClient.getWorkOrder(workOrderId);
            if (result != null && result.getData() != null) {
                WorkOrderVO vo = result.getData();
                return String.format("""
                        工单详情：
                        编号: %s
                        标题: %s
                        描述: %s
                        分类: %s
                        优先级: %s
                        状态: %s
                        创建人: %s
                        处理人: %s
                        AI摘要: %s
                        AI建议: %s
                        创建时间: %s""",
                        vo.getOrderNo(), vo.getTitle(), vo.getDescription(),
                        vo.getCategory(), vo.getPriority(), vo.getStatus(),
                        vo.getCreatorName(), vo.getAssigneeName(),
                        vo.getAiSummary(), vo.getAiSuggestedSolution(),
                        vo.getCreatedAt());
            }
            return "工单不存在";
        } catch (Exception e) {
            log.error("Tool getWorkOrderDetail failed", e);
            return "获取详情失败：" + e.getMessage();
        }
    }

    /**
     * 更新工单状态
     */
    public String updateWorkOrderStatus(Long workOrderId, String status, String comment) {
        log.info("Tool: updateWorkOrderStatus - id={}, status={}", workOrderId, status);
        try {
            WorkOrderStatusUpdateDTO dto = new WorkOrderStatusUpdateDTO();
            dto.setStatus(status);
            dto.setComment(comment);
            R<Void> result = workOrderClient.updateWorkOrderStatus(workOrderId, dto);
            if (result != null && result.getCode() == 0) {
                return String.format("工单 %d 状态已更新为 %s", workOrderId, status);
            }
            return "状态更新失败：" + (result != null ? result.getMessage() : "未知错误");
        } catch (Exception e) {
            log.error("Tool updateWorkOrderStatus failed", e);
            return "状态更新失败：" + e.getMessage();
        }
    }

    /**
     * 搜索知识库
     */
    public String searchKnowledgeBase(String query) {
        log.info("Tool: searchKnowledgeBase - query={}", query);
        try {
            List<KnowledgeSearchResult> results = ragService.retrieve(query, 3);
            if (results.isEmpty()) {
                return "知识库中未找到相关内容";
            }
            StringBuilder sb = new StringBuilder("找到以下相关知识：\n");
            for (int i = 0; i < results.size(); i++) {
                KnowledgeSearchResult r = results.get(i);
                sb.append(String.format("%d. (相似度: %.2f) %s\n", i + 1, r.getScore(), r.getContent()));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Tool searchKnowledgeBase failed", e);
            return "知识库搜索失败：" + e.getMessage();
        }
    }

    /**
     * 获取可用处理人列表
     */
    public String listAvailableAgents(String role) {
        log.info("Tool: listAvailableAgents - role={}", role);
        try {
            R<PageResult<UserVO>> result = userClient.listUsers(role, null);
            if (result != null && result.getData() != null) {
                List<UserVO> users = result.getData().getRecords();
                if (users.isEmpty()) {
                    return "未找到可用的处理人";
                }
                StringBuilder sb = new StringBuilder("可用处理人：\n");
                for (UserVO user : users) {
                    sb.append(String.format("- %s (ID: %d, 部门: %s)\n",
                            user.getRealName(), user.getId(), user.getDepartment()));
                }
                return sb.toString();
            }
            return "获取处理人列表失败";
        } catch (Exception e) {
            log.error("Tool listAvailableAgents failed", e);
            return "获取处理人列表失败：" + e.getMessage();
        }
    }
}
