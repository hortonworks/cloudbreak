package com.sequenceiq.datalake.controller.sdx;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.api.endpoint.sdx.SdxClusterResponse;
import com.sequenceiq.datalake.api.endpoint.sdx.SdxInternalClusterRequest;
import com.sequenceiq.datalake.api.endpoint.sdx.SdxInternalEndpoint;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;

@Controller
public class SdxInternalController implements SdxInternalEndpoint {

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private SdxService sdxService;

    @Override
    public SdxClusterResponse create(String sdxName, @Valid SdxInternalClusterRequest createSdxClusterRequest) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.createSdx(userCrn, sdxName, createSdxClusterRequest, createSdxClusterRequest.getStackV4Request());
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse(sdxCluster.getCrn(), sdxCluster.getClusterName(), sdxCluster.getStatus());
        sdxClusterResponse.setSdxName(sdxCluster.getClusterName());
        return sdxClusterResponse;
    }
}
