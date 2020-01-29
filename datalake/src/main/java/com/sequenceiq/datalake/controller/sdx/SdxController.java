package com.sequenceiq.datalake.controller.sdx;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.cloudbreak.validation.ValidStackNameFormat;
import com.sequenceiq.cloudbreak.validation.ValidStackNameLength;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxRepairService;
import com.sequenceiq.datalake.service.sdx.SdxRetryService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.datalake.service.sdx.start.SdxStartService;
import com.sequenceiq.datalake.service.sdx.stop.SdxStopService;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.RedeploySdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

@Controller
public class SdxController implements SdxEndpoint {

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxRetryService sdxRetryService;

    @Inject
    private SdxRepairService repairService;

    @Inject
    private SdxUpgradeService sdxUpgradeService;

    @Inject
    private SdxClusterConverter sdxClusterConverter;

    @Inject
    private SdxStartService sdxStartService;

    @Inject
    private SdxStopService sdxStopService;

    @Override
    public SdxClusterResponse create(@ValidStackNameFormat @ValidStackNameLength String name, @Valid SdxClusterRequest createSdxClusterRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.createSdx(userCrn, name, createSdxClusterRequest, null);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        sdxClusterResponse.setName(sdxCluster.getClusterName());
        return sdxClusterResponse;
    }

    @Override
    public void delete(String name, Boolean forced) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        sdxService.deleteSdx(userCrn, name, forced);
    }

    @Override
    public void deleteByCrn(String clusterCrn, Boolean forced) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        sdxService.deleteSdxByClusterCrn(userCrn, clusterCrn, forced);
    }

    @Override
    public void redeploy(String envName, @Valid RedeploySdxClusterRequest redeploySdxClusterRequest) {

    }

    @Override
    public void redeployByCrn(String clusterCrn, @Valid RedeploySdxClusterRequest redeploySdxClusterRequest) {

    }

    @Override
    public SdxClusterResponse get(String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        return sdxClusterConverter.sdxClusterToResponse(sdxCluster);
    }

    @Override
    public SdxClusterResponse getByCrn(String clusterCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, clusterCrn);
        return sdxClusterConverter.sdxClusterToResponse(sdxCluster);
    }

    @Override
    public List<SdxClusterResponse> getByEnvCrn(@ValidCrn String envCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<SdxCluster> sdxClusters = sdxService.listSdxByEnvCrn(userCrn, envCrn);
        return sdxClusters.stream()
                .map(sdx -> sdxClusterConverter.sdxClusterToResponse(sdx))
                .collect(Collectors.toList());
    }

    @Override
    public SdxClusterDetailResponse getDetailByCrn(String clusterCrn, Set<String> entries) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, clusterCrn);
        StackV4Response stackV4Response = sdxService.getDetail(sdxCluster.getClusterName(), entries);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        return new SdxClusterDetailResponse(sdxClusterResponse, stackV4Response);
    }

    @Override
    public void repairCluster(String clusterName, SdxRepairRequest clusterRepairRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        repairService.triggerRepairByName(userCrn, clusterName, clusterRepairRequest);
    }

    @Override
    public void repairClusterByCrn(String clusterCrn, SdxRepairRequest clusterRepairRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        repairService.triggerRepairByCrn(userCrn, clusterCrn, clusterRepairRequest);
    }

    @Override
    public UpgradeOptionV4Response checkForUpgradeByName(String clusterName) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxUpgradeService.checkForUpgradeByName(userCrn, clusterName);
    }

    @Override
    public UpgradeOptionV4Response checkForUpgradeByCrn(String clusterCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxUpgradeService.checkForUpgradeByCrn(userCrn, clusterCrn);
    }

    @Override
    public void upgradeClusterByName(String clusterName) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        sdxUpgradeService.triggerUpgradeByName(userCrn, clusterName);
    }

    @Override
    public void upgradeClusterByCrn(String clusterCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        sdxUpgradeService.triggerUpgradeByCrn(userCrn, clusterCrn);
    }

    @Override
    public SdxClusterDetailResponse getDetail(String name, Set<String> entries) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        StackV4Response stackV4Response = sdxService.getDetail(name, entries);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        SdxClusterDetailResponse sdxClusterDetailResponse = new SdxClusterDetailResponse(sdxClusterResponse, stackV4Response);

        return SdxClusterDetailResponseFilter.on(sdxClusterDetailResponse)
            .apply(SdxClusterDetailResponseFilter.REMOVE_NAMENODE_DETAILS)
            .apply(SdxClusterDetailResponseFilter.REMOVE_SOLR_DETAILS)
            .apply(SdxClusterDetailResponseFilter.REMOVE_WEBHDFS_DETAILS)
            .filter();
    }

    @Override
    public List<SdxClusterResponse> list(String envName) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<SdxCluster> sdxClusters = sdxService.listSdx(userCrn, envName);
        return sdxClusters.stream()
                .map(sdx -> sdxClusterConverter.sdxClusterToResponse(sdx))
                .collect(Collectors.toList());
    }

    @Override
    public void sync(String name) {
        sdxService.sync(name);
    }

    @Override
    public void syncByCrn(String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        sdxService.syncByCrn(userCrn, crn);
    }

    @Override
    public void retry(String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        sdxRetryService.retrySdx(sdxCluster);
    }

    @Override
    public void retryByCrn(String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        sdxRetryService.retrySdx(sdxCluster);
    }

    @Override
    public void startByName(String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        sdxStartService.triggerStartIfClusterNotRunning(sdxCluster);
    }

    @Override
    public void startByCrn(String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        sdxStartService.triggerStartIfClusterNotRunning(sdxCluster);
    }

    @Override
    public void stopByName(String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        sdxStopService.triggerStopIfClusterNotStopped(sdxCluster);
    }

    @Override
    public void stopByCrn(String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        sdxStopService.triggerStopIfClusterNotStopped(sdxCluster);
    }

}
