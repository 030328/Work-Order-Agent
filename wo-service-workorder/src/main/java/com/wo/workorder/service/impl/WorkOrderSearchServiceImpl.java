package com.wo.workorder.service.impl;

import com.wo.api.dto.workorder.WorkOrderBriefVO;
import com.wo.common.result.PageResult;
import com.wo.workorder.entity.WoWorkOrder;
import com.wo.workorder.es.WorkOrderDocument;
import com.wo.workorder.service.WorkOrderSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderSearchServiceImpl implements WorkOrderSearchService {

    private static final DateTimeFormatter ES_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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
            log.error("Failed to search work orders in Elasticsearch, keyword={}", keyword, e);
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
            log.info("Indexed work order to Elasticsearch, id={}", workOrder.getId());
        } catch (Exception e) {
            log.error("Failed to index work order to Elasticsearch, id={}", workOrder.getId(), e);
        }
    }

    @Override
    public void deleteIndex(Long id) {
        try {
            elasticsearchOperations.delete(id.toString(), WorkOrderDocument.class);
            log.info("Deleted work order Elasticsearch index, id={}", id);
        } catch (Exception e) {
            log.error("Failed to delete work order Elasticsearch index, id={}", id, e);
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
        doc.setCreatedAt(formatCreatedAt(workOrder.getCreatedAt()));
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
        vo.setCreatedAt(parseCreatedAt(doc.getCreatedAt()));
        return vo;
    }

    private String formatCreatedAt(LocalDateTime createdAt) {
        return createdAt == null ? null : ES_DATE_TIME_FORMATTER.format(createdAt);
    }

    private LocalDateTime parseCreatedAt(String createdAt) {
        if (createdAt == null || createdAt.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(createdAt, ES_DATE_TIME_FORMATTER);
        } catch (Exception dateTimeException) {
            try {
                return LocalDate.parse(createdAt).atStartOfDay();
            } catch (Exception dateException) {
                log.warn("Failed to parse work order Elasticsearch createdAt={}", createdAt);
                return null;
            }
        }
    }
}
