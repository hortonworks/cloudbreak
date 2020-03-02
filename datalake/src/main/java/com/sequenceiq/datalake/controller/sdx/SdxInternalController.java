package com.sequenceiq.datalake.controller.sdx;

import javax.inject.Inject;
import javax.validation.Valid;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxInternalClusterRequest;

@Controller
@AuthorizationResource(type = AuthorizationResourceType.DATALAKE)
public class SdxInternalController implements SdxInternalEndpoint {

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxClusterConverter sdxClusterConverter;

    @Inject
    private SdxMetricService metricService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public SdxClusterResponse create(String name, @Valid SdxInternalClusterRequest createSdxClusterRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        Pair<SdxCluster, FlowIdentifier> result = sdxService.createSdx(userCrn, name, createSdxClusterRequest, createSdxClusterRequest.getStackV4Request());
        SdxCluster sdxCluster = result.getLeft();
        metricService.incrementMetricCounter(MetricType.INTERNAL_SDX_REQUESTED, sdxCluster);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        sdxClusterResponse.setName(sdxCluster.getClusterName());
        sdxClusterResponse.setFlowIdentifier(result.getRight());
        return sdxClusterResponse;
    }
}
