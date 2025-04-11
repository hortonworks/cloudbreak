package com.sequenceiq.periscope.controller;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.periscope.api.endpoint.v1.HistoryEndpoint;
import com.sequenceiq.periscope.api.model.AutoscaleClusterHistoryResponse;
import com.sequenceiq.periscope.converter.HistoryConverter;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;

@Controller
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
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public List<AutoscaleClusterHistoryResponse> getHistoryByCrn(@ResourceCrn String stackCrn, Integer historyCount) {
        return clusterService.findOneByStackCrnAndTenant(stackCrn, restRequestThreadLocalService.getCloudbreakTenant())
                .map(cluster -> historyConverter.convertAllToJson(historyService.getHistory(cluster.getId(), historyCount)))
                .orElseThrow(NotFoundException.notFound("cluster", stackCrn));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public List<AutoscaleClusterHistoryResponse> getHistoryByName(@ResourceName String stackName, Integer historyCount) {
        return clusterService.findOneByStackNameAndTenant(stackName, restRequestThreadLocalService.getCloudbreakTenant())
                .map(cluster -> historyConverter.convertAllToJson(historyService.getHistory(cluster.getId(), historyCount)))
                .orElseThrow(NotFoundException.notFound("cluster", stackName));
    }
}
