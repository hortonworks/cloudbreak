package com.sequenceiq.datalake.controller.sdx;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.api.endpoint.sdx.RedeploySdxClusterRequest;
import com.sequenceiq.datalake.api.endpoint.sdx.SdxClusterRequest;
import com.sequenceiq.datalake.api.endpoint.sdx.SdxClusterResponse;
import com.sequenceiq.datalake.api.endpoint.sdx.SdxEndpoint;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;

@Controller
public class SdxController implements SdxEndpoint {

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private SdxService sdxService;

    @Override
    public SdxClusterResponse create(String sdxName, @Valid SdxClusterRequest createSdxClusterRequest) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.createSdx(userCrn, sdxName, createSdxClusterRequest, null);
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse(sdxCluster.getCrn(), sdxCluster.getClusterName(), sdxCluster.getStatus());
        sdxClusterResponse.setSdxName(sdxCluster.getClusterName());
        return sdxClusterResponse;
    }

    @Override
    public void delete(String sdxName) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        sdxService.deleteSdx(userCrn, sdxName);
    }

    @Override
    public void redeploy(String envName, @Valid RedeploySdxClusterRequest redeploySdxClusterRequest) {

    }

    @Override
    public SdxClusterResponse get(String sdxName) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByAccountIdAndSdxName(userCrn, sdxName);
        return new SdxClusterResponse(sdxCluster.getCrn(), sdxCluster.getClusterName(), sdxCluster.getStatus());
    }

    @Override
    public List<SdxClusterResponse> list(String envName) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        List<SdxCluster> sdxClusters = sdxService.listSdx(userCrn, envName);
        return sdxClusters.stream()
                .map(c -> new SdxClusterResponse(c.getCrn(), c.getClusterName(), c.getStatus()))
                .collect(Collectors.toList());
    }

}
