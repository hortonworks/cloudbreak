package com.sequenceiq.datalake.controller.sdx;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.RedeploySdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Controller
public class SdxController implements SdxEndpoint {

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxClusterConverter sdxClusterConverter;

    @Override
    public SdxClusterResponse create(String name, @Valid SdxClusterRequest createSdxClusterRequest) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.createSdx(userCrn, name, createSdxClusterRequest, null);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        sdxClusterResponse.setName(sdxCluster.getClusterName());
        return sdxClusterResponse;
    }

    @Override
    public void delete(String name) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        sdxService.deleteSdx(userCrn, name);
    }

    @Override
    public void redeploy(String envName, @Valid RedeploySdxClusterRequest redeploySdxClusterRequest) {

    }

    @Override
    public SdxClusterResponse get(String name) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByAccountIdAndSdxName(userCrn, name);
        return sdxClusterConverter.sdxClusterToResponse(sdxCluster);
    }

    @Override
    public SdxClusterDetailResponse getDetail(String name, Set<String> entries) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByAccountIdAndSdxName(userCrn, name);
        StackV4Response stackV4Response = sdxService.getDetail(userCrn, name, entries);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        return new SdxClusterDetailResponse(sdxClusterResponse, stackV4Response);
    }

    @Override
    public List<SdxClusterResponse> list(String envName) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        List<SdxCluster> sdxClusters = sdxService.listSdx(userCrn, envName);
        return sdxClusters.stream()
                .map(sdx -> sdxClusterConverter.sdxClusterToResponse(sdx))
                .collect(Collectors.toList());
    }

}
