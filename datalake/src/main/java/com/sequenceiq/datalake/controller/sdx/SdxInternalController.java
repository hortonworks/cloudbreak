package com.sequenceiq.datalake.controller.sdx;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByEnvironmentName;
import com.sequenceiq.authorization.annotation.EnvironmentName;
import com.sequenceiq.authorization.resource.ResourceType;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.sdx.api.model.SdxInternalClusterRequest;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Controller
@AuthorizationResource(type = ResourceType.DATALAKE)
public class SdxInternalController implements SdxInternalEndpoint {

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxClusterConverter sdxClusterConverter;

    @Override
    @CheckPermissionByEnvironmentName
    public SdxClusterResponse create(String name, @EnvironmentName @Valid SdxInternalClusterRequest createSdxClusterRequest) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.createSdx(userCrn, name, createSdxClusterRequest, createSdxClusterRequest.getStackV4Request());
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        sdxClusterResponse.setName(sdxCluster.getClusterName());
        return sdxClusterResponse;
    }
}
