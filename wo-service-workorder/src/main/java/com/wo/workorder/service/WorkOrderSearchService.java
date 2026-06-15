package com.wo.workorder.service;

import com.wo.api.dto.workorder.WorkOrderBriefVO;
import com.wo.common.result.PageResult;
import com.wo.workorder.entity.WoWorkOrder;

public interface WorkOrderSearchService {

    /**
     * 全文搜索工单
     */
    PageResult<WorkOrderBriefVO> search(String keyword, int page, int size);

    /**
     * 索引工单到ES
     */
    void indexWorkOrder(WoWorkOrder workOrder);

    /**
     * 从ES删除工单索引
     */
    void deleteIndex(Long id);
}
