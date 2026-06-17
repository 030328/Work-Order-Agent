package com.wo.workorder.service.impl;

import com.wo.api.client.UserClient;
import com.wo.api.dto.workorder.WorkOrderBriefVO;
import com.wo.common.result.PageResult;
import com.wo.workorder.entity.WoWorkOrder;
import com.wo.workorder.service.WorkOrderSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class WorkOrderSearchServiceImpl implements WorkOrderSearchService {

    @Autowired(required = false)
    private UserClient userClient;

    @Override
    public PageResult<WorkOrderBriefVO> search(String keyword, int page, int size) {
        log.warn("ES搜索暂未启用, keyword={}", keyword);
        return PageResult.of(0, page, size, List.of());
    }

    @Override
    public void indexWorkOrder(WoWorkOrder workOrder) {
        log.debug("ES索引暂未启用, 跳过索引工单 id={}", workOrder.getId());
    }

    @Override
    public void deleteIndex(Long id) {
        log.debug("ES索引暂未启用, 跳过删除索引 id={}", id);
    }
}
