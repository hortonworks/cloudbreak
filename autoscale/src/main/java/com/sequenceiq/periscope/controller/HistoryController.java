package com.sequenceiq.periscope.controller;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.endpoint.v1.HistoryEndpoint;
import com.sequenceiq.periscope.api.model.AutoscaleClusterHistoryResponse;
import com.sequenceiq.periscope.converter.HistoryConverter;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.NotFoundException;

@Component
public class HistoryController implements HistoryEndpoint {

    @Inject
    private HistoryService historyService;

    @Inject
    private HistoryConverter historyConverter;

    @Inject
    private AutoscaleRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ClusterService clusterService;

    @Override
    public List<AutoscaleClusterHistoryResponse> getHistoryByCrn(String stackCrn, Integer historyCount) {
        return clusterService.findOneByStackCrnAndWorkspaceId(stackCrn, restRequestThreadLocalService.getRequestedWorkspaceId())
                .map(cluster -> historyConverter.convertAllToJson(historyService.getHistory(cluster.getId(), historyCount)))
                .orElseThrow(NotFoundException.notFound("cluster", stackCrn));
    }

    @Override
    public List<AutoscaleClusterHistoryResponse> getHistoryByName(String stackName, Integer historyCount) {
        return clusterService.findOneByStackNameAndWorkspaceId(stackName, restRequestThreadLocalService.getRequestedWorkspaceId())
                .map(cluster -> historyConverter.convertAllToJson(historyService.getHistory(cluster.getId(), historyCount)))
                .orElseThrow(NotFoundException.notFound("cluster", stackName));
    }
}
