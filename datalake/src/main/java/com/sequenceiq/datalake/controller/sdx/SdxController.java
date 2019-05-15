package com.sequenceiq.datalake.controller.sdx;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.datalake.api.endpoint.sdx.RedeploySdxClusterRequest;
import com.sequenceiq.datalake.api.endpoint.sdx.SdxClusterRequest;
import com.sequenceiq.datalake.api.endpoint.sdx.SdxClusterResponse;
import com.sequenceiq.datalake.api.endpoint.sdx.SdxEndpoint;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.util.RestRequestThreadLocalService;

@Controller
public class SdxController implements SdxEndpoint {

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private SdxService sdxService;

    @Override
    public SdxClusterResponse create(String envName, @Valid SdxClusterRequest createSdxClusterRequest) {
        String userCrn = restRequestThreadLocalService.getUserCrn();
        SdxCluster sdxCluster = sdxService.createSdx(userCrn, envName, createSdxClusterRequest);
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse(sdxCluster.getClusterName(), sdxCluster.getStatus());
        sdxClusterResponse.setSdxName(sdxCluster.getClusterName());
        return sdxClusterResponse;

    }

    @Override
    public void delete(String envName) {
        String userCrn = restRequestThreadLocalService.getUserCrn();
        sdxService.deleteSdx(userCrn, envName);
    }

    @Override
    public void redeploy(String envName, @Valid RedeploySdxClusterRequest redeploySdxClusterRequest) {

    }

    @Override
    public SdxClusterResponse get(String envName) {
        String userCrn = restRequestThreadLocalService.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByAccountIdAndEnvName(userCrn, envName);
        return new SdxClusterResponse(sdxCluster.getClusterName(), sdxCluster.getStatus());
    }

    @Override
    public List<SdxClusterResponse> list(String envName) {
        String userCrn = restRequestThreadLocalService.getUserCrn();
        List<SdxCluster> sdxClusters = sdxService.listSdx(userCrn, envName);
        return sdxClusters.stream()
                .map(c -> new SdxClusterResponse(c.getClusterName(), c.getStatus()))
                .collect(Collectors.toList());
    }

}
