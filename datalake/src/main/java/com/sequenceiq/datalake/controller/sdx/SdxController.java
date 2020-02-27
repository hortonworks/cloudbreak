package com.sequenceiq.datalake.controller.sdx;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.cloudbreak.validation.ValidStackNameFormat;
import com.sequenceiq.cloudbreak.validation.ValidStackNameLength;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.sdx.SdxRepairService;
import com.sequenceiq.datalake.service.sdx.SdxRetryService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.datalake.service.sdx.start.SdxStartService;
import com.sequenceiq.datalake.service.sdx.stop.SdxStopService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
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

    @Inject
    private SdxMetricService metricService;

    @Override
    public SdxClusterResponse create(@ValidStackNameFormat @ValidStackNameLength String name, @Valid SdxClusterRequest createSdxClusterRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        Pair<SdxCluster, FlowIdentifier> result = sdxService.createSdx(userCrn, name, createSdxClusterRequest, null);
        SdxCluster sdxCluster = result.getLeft();
        metricService.incrementMetricCounter(MetricType.EXTERNAL_SDX_REQUESTED, sdxCluster);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        sdxClusterResponse.setName(sdxCluster.getClusterName());
        sdxClusterResponse.setFlowIdentifier(result.getRight());
        return sdxClusterResponse;
    }

    @Override
    public FlowIdentifier delete(String name, Boolean forced) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxService.deleteSdx(userCrn, name, forced);
    }

    @Override
    public FlowIdentifier deleteByCrn(String clusterCrn, Boolean forced) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxService.deleteSdxByClusterCrn(userCrn, clusterCrn, forced);
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
    public FlowIdentifier repairCluster(String clusterName, SdxRepairRequest clusterRepairRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return repairService.triggerRepairByName(userCrn, clusterName, clusterRepairRequest);
    }

    @Override
    public FlowIdentifier repairClusterByCrn(String clusterCrn, SdxRepairRequest clusterRepairRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return repairService.triggerRepairByCrn(userCrn, clusterCrn, clusterRepairRequest);
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
    public FlowIdentifier upgradeClusterByName(String clusterName) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxUpgradeService.triggerUpgradeByName(userCrn, clusterName);
    }

    @Override
    public FlowIdentifier upgradeClusterByCrn(String clusterCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxUpgradeService.triggerUpgradeByCrn(userCrn, clusterCrn);
    }

    @Override
    public SdxClusterDetailResponse getDetail(String name, Set<String> entries) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        StackV4Response stackV4Response = sdxService.getDetail(name, entries);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        return new SdxClusterDetailResponse(sdxClusterResponse, stackV4Response);
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
    public FlowIdentifier retry(String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        return sdxRetryService.retrySdx(sdxCluster);
    }

    @Override
    public FlowIdentifier retryByCrn(String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        return sdxRetryService.retrySdx(sdxCluster);
    }

    @Override
    public FlowIdentifier startByName(String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        return sdxStartService.triggerStartIfClusterNotRunning(sdxCluster);
    }

    @Override
    public FlowIdentifier startByCrn(String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        return sdxStartService.triggerStartIfClusterNotRunning(sdxCluster);
    }

    @Override
    public FlowIdentifier stopByName(String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        return sdxStopService.triggerStopIfClusterNotStopped(sdxCluster);
    }

    @Override
    public FlowIdentifier stopByCrn(String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        return sdxStopService.triggerStopIfClusterNotStopped(sdxCluster);
    }

    @Override
    public List<String> versions() {
        return sdxService.getDatalakeVersions();
    }

}
