package com.sequenceiq.datalake.controller.sdx;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByEnvironmentCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByEnvironmentName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.EnvironmentCrn;
import com.sequenceiq.authorization.annotation.EnvironmentName;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
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
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.RedeploySdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

@Controller
@AuthorizationResource(type = AuthorizationResourceType.DATALAKE)
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
    @CheckPermissionByEnvironmentName(action = AuthorizationResourceAction.WRITE)
    public SdxClusterResponse create(@ValidStackNameFormat @ValidStackNameLength String name,
            @EnvironmentName @Valid SdxClusterRequest createSdxClusterRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.createSdx(userCrn, name, createSdxClusterRequest, null);
        metricService.incrementMetricCounter(MetricType.EXTERNAL_SDX_REQUESTED, sdxCluster);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        sdxClusterResponse.setName(sdxCluster.getClusterName());
        return sdxClusterResponse;
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.WRITE)
    public void delete(@ResourceName String name, Boolean forced) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        sdxService.deleteSdx(userCrn, name, forced);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.WRITE)
    public void deleteByCrn(@ResourceCrn String clusterCrn, Boolean forced) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        sdxService.deleteSdxByClusterCrn(userCrn, clusterCrn, forced);
    }

    @Override
    @CheckPermissionByEnvironmentName(action = AuthorizationResourceAction.WRITE)
    public void redeploy(@EnvironmentName String envName, @Valid RedeploySdxClusterRequest redeploySdxClusterRequest) {

    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.WRITE)
    public void redeployByCrn(@ResourceCrn String clusterCrn, @Valid RedeploySdxClusterRequest redeploySdxClusterRequest) {

    }

    @Override
    @CheckPermissionByResourceName
    public SdxClusterResponse get(@ResourceName String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        return sdxClusterConverter.sdxClusterToResponse(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceCrn
    public SdxClusterResponse getByCrn(@ResourceCrn String clusterCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, clusterCrn);
        return sdxClusterConverter.sdxClusterToResponse(sdxCluster);
    }

    @Override
    @CheckPermissionByEnvironmentCrn
    public List<SdxClusterResponse> getByEnvCrn(@EnvironmentCrn @ValidCrn String envCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<SdxCluster> sdxClusters = sdxService.listSdxByEnvCrn(userCrn, envCrn);
        return sdxClusters.stream()
                .map(sdx -> sdxClusterConverter.sdxClusterToResponse(sdx))
                .collect(Collectors.toList());
    }

    @Override
    @CheckPermissionByResourceCrn
    public SdxClusterDetailResponse getDetailByCrn(@ResourceCrn String clusterCrn, Set<String> entries) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, clusterCrn);
        StackV4Response stackV4Response = sdxService.getDetail(sdxCluster.getClusterName(), entries);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        return new SdxClusterDetailResponse(sdxClusterResponse, stackV4Response);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.WRITE)
    public void repairCluster(@ResourceName String clusterName, SdxRepairRequest clusterRepairRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        repairService.triggerRepairByName(userCrn, clusterName, clusterRepairRequest);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.WRITE)
    public void repairClusterByCrn(@ResourceCrn String clusterCrn, SdxRepairRequest clusterRepairRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        repairService.triggerRepairByCrn(userCrn, clusterCrn, clusterRepairRequest);
    }

    @Override
    @CheckPermissionByResourceName
    public UpgradeOptionV4Response checkForUpgradeByName(@ResourceName String clusterName) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxUpgradeService.checkForUpgradeByName(userCrn, clusterName);
    }

    @Override
    @CheckPermissionByResourceCrn
    public UpgradeOptionV4Response checkForUpgradeByCrn(@ResourceCrn String clusterCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxUpgradeService.checkForUpgradeByCrn(userCrn, clusterCrn);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.WRITE)
    public void upgradeClusterByName(@ResourceName String clusterName) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        sdxUpgradeService.triggerUpgradeByName(userCrn, clusterName);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.WRITE)
    public void upgradeClusterByCrn(@ResourceCrn String clusterCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        sdxUpgradeService.triggerUpgradeByCrn(userCrn, clusterCrn);
    }

    @Override
    @CheckPermissionByResourceName
    public SdxClusterDetailResponse getDetail(@ResourceName String name, Set<String> entries) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        StackV4Response stackV4Response = sdxService.getDetail(name, entries);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        return new SdxClusterDetailResponse(sdxClusterResponse, stackV4Response);
    }

    @Override
    @DisableCheckPermissions
    public List<SdxClusterResponse> list(String envName) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<SdxCluster> sdxClusters = sdxService.listSdx(userCrn, envName);
        return sdxClusters.stream()
                .map(sdx -> sdxClusterConverter.sdxClusterToResponse(sdx))
                .collect(Collectors.toList());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.WRITE)
    public void sync(@ResourceName String name) {
        sdxService.sync(name);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.WRITE)
    public void syncByCrn(@ResourceCrn String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        sdxService.syncByCrn(userCrn, crn);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.WRITE)
    public void retry(@ResourceName String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        sdxRetryService.retrySdx(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.WRITE)
    public void retryByCrn(@ResourceCrn String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        sdxRetryService.retrySdx(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.WRITE)
    public void startByName(@ResourceName String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        sdxStartService.triggerStartIfClusterNotRunning(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.WRITE)
    public void startByCrn(@ResourceCrn String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        sdxStartService.triggerStartIfClusterNotRunning(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.WRITE)
    public void stopByName(@ResourceName String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        sdxStopService.triggerStopIfClusterNotStopped(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.WRITE)
    public void stopByCrn(@ResourceCrn String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        sdxStopService.triggerStopIfClusterNotStopped(sdxCluster);
    }

    @Override
    public List<String> versions() {
        return sdxService.getDatalakeVersions();
    }

}
