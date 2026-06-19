package com.wo.workorder.service.impl;

import com.wo.api.dto.workorder.WorkOrderBriefVO;
import com.wo.common.result.PageResult;
import com.wo.workorder.entity.WoWorkOrder;
import com.wo.workorder.es.WorkOrderDocument;
import com.wo.workorder.service.WorkOrderSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderSearchServiceImpl implements WorkOrderSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public PageResult<WorkOrderBriefVO> search(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) {
            return PageResult.of(0, page, size, List.of());
        }

        try {
            Criteria criteria = new Criteria("title").matches(keyword)
                    .or(new Criteria("description").matches(keyword));
            CriteriaQuery query = new CriteriaQuery(criteria);
            query.setPageable(org.springframework.data.domain.PageRequest.of(page - 1, size));

            SearchHits<WorkOrderDocument> hits = elasticsearchOperations.search(query, WorkOrderDocument.class);

            List<WorkOrderBriefVO> voList = hits.getSearchHits().stream()
                    .map(hit -> convertToBriefVO(hit.getContent()))
                    .collect(Collectors.toList());

            return PageResult.of(hits.getTotalHits(), page, size, voList);
        } catch (Exception e) {
            log.error("ES搜索失败, keyword={}", keyword, e);
            return PageResult.of(0, page, size, List.of());
        }
    }

    @Override
    public void indexWorkOrder(WoWorkOrder workOrder) {
        try {
            WorkOrderDocument doc = convertToDocument(workOrder);
            IndexQuery indexQuery = new IndexQueryBuilder()
                    .withId(doc.getId().toString())
                    .withObject(doc)
                    .build();
            elasticsearchOperations.index(indexQuery, elasticsearchOperations.getIndexCoordinatesFor(WorkOrderDocument.class));
            log.info("工单索引到ES成功, id={}", workOrder.getId());
        } catch (Exception e) {
            log.error("工单索引到ES失败, id={}", workOrder.getId(), e);
        }
    }

    @Override
    public void deleteIndex(Long id) {
        try {
            elasticsearchOperations.delete(id.toString(), WorkOrderDocument.class);
            log.info("ES索引删除成功, id={}", id);
        } catch (Exception e) {
            log.error("ES索引删除失败, id={}", id, e);
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
        doc.setResolution(workOrder.getResolution());
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
        return vo;
    }
}
