package com.sequenceiq.datalake.controller.sdx;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.cloudbreak.validation.ValidStackNameFormat;
import com.sequenceiq.cloudbreak.validation.ValidStackNameLength;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxRepairService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.RedeploySdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

@Controller
public class SdxController implements SdxEndpoint {

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxRepairService repairService;

    @Inject
    private SdxClusterConverter sdxClusterConverter;

    @Override
    public SdxClusterResponse create(@ValidStackNameFormat @ValidStackNameLength String name, @Valid SdxClusterRequest createSdxClusterRequest) {
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
    public void deleteByCrn(String clusterCrn) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        sdxService.deleteSdxByClusterCrn(userCrn, clusterCrn);
    }

    @Override
    public void redeploy(String envName, @Valid RedeploySdxClusterRequest redeploySdxClusterRequest) {

    }

    @Override
    public void redeployByCrn(String clusterCrn, @Valid RedeploySdxClusterRequest redeploySdxClusterRequest) {

    }

    @Override
    public SdxClusterResponse get(String name) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        return sdxClusterConverter.sdxClusterToResponse(sdxCluster);
    }

    @Override
    public SdxClusterResponse getByCrn(String clusterCrn) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, clusterCrn);
        return sdxClusterConverter.sdxClusterToResponse(sdxCluster);
    }

    @Override
    public List<SdxClusterResponse> getByEnvCrn(@ValidCrn String envCrn) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        List<SdxCluster> sdxClusters = sdxService.listSdxByEnvCrn(userCrn, envCrn);
        return sdxClusters.stream()
                .map(sdx -> sdxClusterConverter.sdxClusterToResponse(sdx))
                .collect(Collectors.toList());
    }

    @Override
    public SdxClusterDetailResponse getDetailByCrn(String clusterCrn, Set<String> entries) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, clusterCrn);
        StackV4Response stackV4Response = sdxService.getDetail(userCrn, sdxCluster.getClusterName(), entries);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        return new SdxClusterDetailResponse(sdxClusterResponse, stackV4Response);
    }

    @Override
    public void repairCluster(String clusterName, SdxRepairRequest clusterRepairRequest) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        repairService.triggerRepairByName(userCrn, clusterName, clusterRepairRequest);
    }

    @Override
    public void repairClusterByCrn(String clusterCrn, SdxRepairRequest clusterRepairRequest) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        repairService.triggerRepairByCrn(userCrn, clusterCrn, clusterRepairRequest);
    }

    @Override
    public SdxClusterDetailResponse getDetail(String name, Set<String> entries) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
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
