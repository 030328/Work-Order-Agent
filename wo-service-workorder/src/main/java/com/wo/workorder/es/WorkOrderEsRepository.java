package com.wo.workorder.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface WorkOrderEsRepository extends ElasticsearchRepository<WorkOrderDocument, Long> {

    List<WorkOrderDocument> findByTitleContainingOrDescriptionContaining(String title, String desc);
}
