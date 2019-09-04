package com.sequenceiq.datalake.controller.sdx;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByEnvironmentName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.EnvironmentName;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.authorization.resource.ResourceType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.cloudbreak.validation.ValidStackNameFormat;
import com.sequenceiq.cloudbreak.validation.ValidStackNameLength;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxRepairService;
import com.sequenceiq.datalake.service.sdx.SdxRetryService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.RedeploySdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

@Controller
@AuthorizationResource(type = ResourceType.DATALAKE)
public class SdxController implements SdxEndpoint {

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxRetryService sdxRetryService;

    @Inject
    private SdxRepairService repairService;

    @Inject
    private SdxClusterConverter sdxClusterConverter;

    @Override
    @CheckPermissionByEnvironmentName(action = ResourceAction.WRITE)
    public SdxClusterResponse create(@ValidStackNameFormat @ValidStackNameLength String name,
            @EnvironmentName @Valid SdxClusterRequest createSdxClusterRequest) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.createSdx(userCrn, name, createSdxClusterRequest, null);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        sdxClusterResponse.setName(sdxCluster.getClusterName());
        return sdxClusterResponse;
    }

    @Override
    @CheckPermissionByResourceName(action = ResourceAction.WRITE, relatedResourceClass = SdxCluster.class)
    public void delete(@ResourceName String name) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        sdxService.deleteSdx(userCrn, name);
    }

    @Override
    @CheckPermissionByResourceCrn(action = ResourceAction.WRITE, relatedResourceClass = SdxCluster.class)
    public void deleteByCrn(@ResourceCrn String clusterCrn) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        sdxService.deleteSdxByClusterCrn(userCrn, clusterCrn);
    }

    @Override
    @CheckPermissionByEnvironmentName(action = ResourceAction.WRITE)
    public void redeploy(@EnvironmentName String envName, @Valid RedeploySdxClusterRequest redeploySdxClusterRequest) {

    }

    @Override
    @CheckPermissionByResourceCrn(action = ResourceAction.WRITE, relatedResourceClass = SdxCluster.class)
    public void redeployByCrn(@ResourceCrn String clusterCrn, @Valid RedeploySdxClusterRequest redeploySdxClusterRequest) {

    }

    @Override
    @DisableCheckPermissions
    public SdxClusterResponse get(String name) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        return sdxClusterConverter.sdxClusterToResponse(sdxCluster);
    }

    @Override
    @DisableCheckPermissions
    public SdxClusterResponse getByCrn(String clusterCrn) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, clusterCrn);
        return sdxClusterConverter.sdxClusterToResponse(sdxCluster);
    }

    @Override
    @DisableCheckPermissions
    public List<SdxClusterResponse> getByEnvCrn(@ValidCrn String envCrn) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        List<SdxCluster> sdxClusters = sdxService.listSdxByEnvCrn(userCrn, envCrn);
        return sdxClusters.stream()
                .map(sdx -> sdxClusterConverter.sdxClusterToResponse(sdx))
                .collect(Collectors.toList());
    }

    @Override
    @DisableCheckPermissions
    public SdxClusterDetailResponse getDetailByCrn(String clusterCrn, Set<String> entries) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, clusterCrn);
        StackV4Response stackV4Response = sdxService.getDetail(sdxCluster.getClusterName(), entries);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        return new SdxClusterDetailResponse(sdxClusterResponse, stackV4Response);
    }

    @Override
    @CheckPermissionByResourceName(action = ResourceAction.WRITE, relatedResourceClass = SdxCluster.class)
    public void repairCluster(String clusterName, SdxRepairRequest clusterRepairRequest) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        repairService.triggerRepairByName(userCrn, clusterName, clusterRepairRequest);
    }

    @Override
    @CheckPermissionByResourceCrn(action = ResourceAction.WRITE, relatedResourceClass = SdxCluster.class)
    public void repairClusterByCrn(@ResourceCrn String clusterCrn, SdxRepairRequest clusterRepairRequest) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        repairService.triggerRepairByCrn(userCrn, clusterCrn, clusterRepairRequest);
    }

    @Override
    @DisableCheckPermissions
    public SdxClusterDetailResponse getDetail(String name, Set<String> entries) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        StackV4Response stackV4Response = sdxService.getDetail(name, entries);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        return new SdxClusterDetailResponse(sdxClusterResponse, stackV4Response);
    }

    @Override
    @DisableCheckPermissions
    public List<SdxClusterResponse> list(String envName) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        List<SdxCluster> sdxClusters = sdxService.listSdx(userCrn, envName);
        return sdxClusters.stream()
                .map(sdx -> sdxClusterConverter.sdxClusterToResponse(sdx))
                .collect(Collectors.toList());
    }

    @Override
    @CheckPermissionByResourceName(action = ResourceAction.WRITE, relatedResourceClass = SdxCluster.class)
    public void sync(@ResourceName String name) {
        sdxService.sync(name);
    }

    @Override
    @CheckPermissionByResourceCrn(action = ResourceAction.WRITE, relatedResourceClass = SdxCluster.class)
    public void syncByCrn(@ResourceCrn String crn) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        sdxService.syncByCrn(userCrn, crn);
    }

    @Override
    public void retry(String name) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, name);
        sdxRetryService.retrySdx(sdxCluster);
    }

    @Override
    public void retryByCrn(String crn) {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        sdxRetryService.retrySdx(sdxCluster);
    }

}
