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
    public List<AutoscaleClusterHistoryResponse> getHistory(String stackCrn) {
        return clusterService.findOneByStackCrnAndTenant(stackCrn, restRequestThreadLocalService.getCloudbreakTenant())
                .map(cluster -> historyConverter.convertAllToJson(historyService.getHistory(cluster.getId())))
                .orElseThrow(NotFoundException.notFound("cluster", stackCrn));
    }
}
