package com.sequenceiq.periscope.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.endpoint.v1.HistoryEndpoint;
import com.sequenceiq.periscope.api.model.AutoscaleClusterHistoryResponse;
import com.sequenceiq.periscope.converter.HistoryConverter;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.service.HistoryService;

@Component
public class HistoryController implements HistoryEndpoint {

    @Autowired
    private HistoryService historyService;

    @Autowired
    private HistoryConverter historyConverter;

    @Override
    public List<AutoscaleClusterHistoryResponse> getHistory(Long clusterId) {
        List<History> history = historyService.getHistory(clusterId);
        return historyConverter.convertAllToJson(history);
    }

    @Override
    public AutoscaleClusterHistoryResponse getHistoryById(Long clusterId, Long historyId) {
        History history = historyService.getHistory(clusterId, historyId);
        return historyConverter.convert(history);
    }
}
