package com.wo.workorder.service.impl;

import com.wo.api.client.UserClient;
import com.wo.api.dto.workorder.WorkOrderBriefVO;
import com.wo.common.result.PageResult;
import com.wo.workorder.entity.WoWorkOrder;
import com.wo.workorder.es.WorkOrderDocument;
import com.wo.workorder.es.WorkOrderEsRepository;
import com.wo.workorder.service.WorkOrderSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderSearchServiceImpl implements WorkOrderSearchService {

    private final WorkOrderEsRepository workOrderEsRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final UserClient userClient;

    @Override
    public PageResult<WorkOrderBriefVO> search(String keyword, int page, int size) {
        if (!StringUtils.hasText(keyword)) {
            return PageResult.of(0, page, size, List.of());
        }

        // 使用Repository进行简单搜索
        List<WorkOrderDocument> documents = workOrderEsRepository
                .findByTitleContainingOrDescriptionContaining(keyword, keyword);

        // 手动分页
        int start = (page - 1) * size;
        int end = Math.min(start + size, documents.size());
        List<WorkOrderDocument> pagedDocs = start < documents.size()
                ? documents.subList(start, end)
                : List.of();

        List<WorkOrderBriefVO> voList = pagedDocs.stream()
                .map(this::convertToBriefVO)
                .collect(Collectors.toList());

        return PageResult.of(documents.size(), page, size, voList);
    }

    @Override
    public void indexWorkOrder(WoWorkOrder workOrder) {
        try {
            WorkOrderDocument document = convertToDocument(workOrder);
            workOrderEsRepository.save(document);
            log.debug("工单索引成功, id={}", workOrder.getId());
        } catch (Exception e) {
            log.error("工单索引失败, id={}", workOrder.getId(), e);
        }
    }

    @Override
    public void deleteIndex(Long id) {
        try {
            workOrderEsRepository.deleteById(id);
            log.debug("工单索引删除成功, id={}", id);
        } catch (Exception e) {
            log.error("工单索引删除失败, id={}", id, e);
        }
    }

    private WorkOrderDocument convertToDocument(WoWorkOrder workOrder) {
        WorkOrderDocument doc = new WorkOrderDocument();
        doc.setId(workOrder.getId());
        doc.setOrderNo(workOrder.getOrderNo());
        doc.setTitle(workOrder.getTitle());
        doc.setDescription(workOrder.getDescription());
        doc.setCategory(workOrder.getCategory());
        doc.setPriority(workOrder.getPriority());
        doc.setStatus(workOrder.getStatus());
        doc.setCreatorId(workOrder.getCreatorId());
        doc.setAssigneeId(workOrder.getAssigneeId());
        doc.setTags(workOrder.getTags());
        doc.setCreatedAt(workOrder.getCreatedAt());
        return doc;
    }

    private WorkOrderBriefVO convertToBriefVO(WorkOrderDocument doc) {
        WorkOrderBriefVO vo = new WorkOrderBriefVO();
        vo.setId(doc.getId());
        vo.setOrderNo(doc.getOrderNo());
        vo.setTitle(doc.getTitle());
        vo.setCategory(doc.getCategory());
        vo.setPriority(doc.getPriority());
        vo.setStatus(doc.getStatus());
        vo.setCreatedAt(doc.getCreatedAt());

        // 获取处理人名称
        if (doc.getAssigneeId() != null) {
            try {
                var resp = userClient.getUserInfo(doc.getAssigneeId());
                if (resp != null && resp.getData() != null) {
                    vo.setAssigneeName(resp.getData().getRealName());
                }
            } catch (Exception e) {
                log.warn("获取用户信息失败, userId={}", doc.getAssigneeId(), e);
            }
        }

        return vo;
    }
}
