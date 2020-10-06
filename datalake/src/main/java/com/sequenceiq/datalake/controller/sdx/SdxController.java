package com.sequenceiq.datalake.controller.sdx;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.cloudbreak.validation.ValidStackNameFormat;
import com.sequenceiq.cloudbreak.validation.ValidStackNameLength;
import com.sequenceiq.datalake.cm.RangerCloudIdentityService;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.sdx.SdxRepairService;
import com.sequenceiq.datalake.service.sdx.SdxRetryService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.dr.SdxDatabaseDrService;
import com.sequenceiq.datalake.service.sdx.start.SdxStartService;
import com.sequenceiq.datalake.service.sdx.stop.SdxStopService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.AdvertisedRuntime;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncStatus;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreStatusResponse;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;
import com.sequenceiq.sdx.api.model.SetRangerCloudIdentityMappingRequest;

@Controller
public class SdxController implements SdxEndpoint {

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxRetryService sdxRetryService;

    @Inject
    private SdxRepairService repairService;

    @Inject
    private SdxClusterConverter sdxClusterConverter;

    @Inject
    private SdxStartService sdxStartService;

    @Inject
    private SdxStopService sdxStopService;

    @Inject
    private CDPConfigService cdpConfigService;

    @Inject
    private SdxMetricService metricService;

    @Inject
    private SdxDatabaseDrService sdxDatabaseDrService;

    @Inject
    private RangerCloudIdentityService rangerCloudIdentityService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATALAKE)
    public SdxClusterResponse create(@ValidStackNameFormat @ValidStackNameLength String name,
            @Valid SdxClusterRequest createSdxClusterRequest) {
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
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_DATALAKE)
    public FlowIdentifier delete(@ResourceName String name, Boolean forced) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxService.deleteSdx(userCrn, name, forced);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_DATALAKE)
    public FlowIdentifier deleteByCrn(@ResourceCrn String clusterCrn, Boolean forced) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxService.deleteSdxByClusterCrn(userCrn, clusterCrn, forced);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_DATALAKE)
    public SdxClusterResponse get(@ResourceName String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByNameInAccount(userCrn, name);
        return sdxClusterConverter.sdxClusterToResponse(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATALAKE)
    public SdxClusterResponse getByCrn(@TenantAwareParam @ResourceCrn String clusterCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, clusterCrn);
        return sdxClusterConverter.sdxClusterToResponse(sdxCluster);
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
    @DisableCheckPermissions
    public List<SdxClusterResponse> getByEnvCrn(@ValidCrn String envCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<SdxCluster> sdxClusters = sdxService.listSdxByEnvCrn(userCrn, envCrn);
        return sdxClusters.stream()
                .map(sdx -> sdxClusterConverter.sdxClusterToResponse(sdx))
                .collect(Collectors.toList());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_DETAILED_DATALAKE)
    public SdxClusterDetailResponse getDetail(@ResourceName String name, Set<String> entries) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByNameInAccount(userCrn, name);
        StackV4Response stackV4Response = sdxService.getDetail(name, entries, sdxCluster.getAccountId());
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        return new SdxClusterDetailResponse(sdxClusterResponse, stackV4Response);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DETAILED_DATALAKE)
    public SdxClusterDetailResponse getDetailByCrn(@TenantAwareParam @ResourceCrn String clusterCrn, Set<String> entries) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, clusterCrn);
        StackV4Response stackV4Response = sdxService.getDetail(sdxCluster.getClusterName(), entries, sdxCluster.getAccountId());
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        return new SdxClusterDetailResponse(sdxClusterResponse, stackV4Response);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REPAIR_DATALAKE)
    public FlowIdentifier repairCluster(@ResourceName String clusterName, SdxRepairRequest clusterRepairRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return repairService.triggerRepairByName(userCrn, clusterName, clusterRepairRequest);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REPAIR_DATALAKE)
    public FlowIdentifier repairClusterByCrn(@ResourceCrn String clusterCrn, SdxRepairRequest clusterRepairRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return repairService.triggerRepairByCrn(userCrn, clusterCrn, clusterRepairRequest);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SYNC_DATALAKE)
    public void sync(@ResourceName String name) {
        sdxService.sync(name, ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SYNC_DATALAKE)
    public void syncByCrn(@ResourceCrn String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        sdxService.syncByCrn(userCrn, crn);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RETRY_DATALAKE_OPERATION)
    public FlowIdentifier retry(@ResourceName String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByNameInAccount(userCrn, name);
        return sdxRetryService.retrySdx(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.RETRY_DATALAKE_OPERATION)
    public FlowIdentifier retryByCrn(@ResourceCrn String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        return sdxRetryService.retrySdx(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.START_DATALAKE)
    public FlowIdentifier startByName(@ResourceName String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByNameInAccount(userCrn, name);
        return sdxStartService.triggerStartIfClusterNotRunning(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.START_DATALAKE)
    public FlowIdentifier startByCrn(@ResourceCrn String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        return sdxStartService.triggerStartIfClusterNotRunning(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.STOP_DATALAKE)
    public FlowIdentifier stopByName(@ResourceName String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByNameInAccount(userCrn, name);
        return sdxStopService.triggerStopIfClusterNotStopped(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.STOP_DATALAKE)
    public FlowIdentifier stopByCrn(@ResourceCrn String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        return sdxStopService.triggerStopIfClusterNotStopped(sdxCluster);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATALAKE)
    public List<String> versions() {
        return cdpConfigService.getDatalakeVersions();
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATALAKE)
    public List<AdvertisedRuntime> advertisedRuntimes() {
        return cdpConfigService.getAdvertisedRuntimes();
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.BACKUP_DATALAKE)
    public SdxDatabaseBackupResponse backupDatabaseByName(@ResourceName String name, String backupId, String backupLocation) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByNameInAccount(userCrn, name);
        return sdxDatabaseDrService.triggerDatabaseBackup(sdxCluster, backupId, backupLocation);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.RESTORE_DATALAKE)
    public SdxDatabaseRestoreResponse restoreDatabaseByName(@ResourceName String name, String backupId, String backupLocation) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByNameInAccount(userCrn, name);
        return sdxDatabaseDrService.triggerDatabaseRestore(sdxCluster, backupId, backupLocation);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.BACKUP_DATALAKE)
    public SdxDatabaseBackupStatusResponse getBackupDatabaseStatusByName(@ResourceName String name, String operationId) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByNameInAccount(userCrn, name);
        return sdxDatabaseDrService.getDatabaseBackupStatus(sdxCluster, operationId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.RESTORE_DATALAKE)
    public SdxDatabaseRestoreStatusResponse getRestoreDatabaseStatusByName(@ResourceName String name, String operationId) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByNameInAccount(userCrn, name);
        return sdxDatabaseDrService.getDatabaseRestoreStatus(sdxCluster, operationId);    }

    @Override
    @InternalOnly
    public RangerCloudIdentitySyncStatus setRangerCloudIdentityMapping(String envCrn, SetRangerCloudIdentityMappingRequest request) {
        if (request.getAzureGroupMapping() != null) {
            throw new IllegalArgumentException("Azure group mappings is unsupported");
        }
        return rangerCloudIdentityService.setAzureCloudIdentityMapping(envCrn, request.getAzureUserMapping());
    }

    @Override
    @InternalOnly
    public RangerCloudIdentitySyncStatus getRangerCloudIdentitySyncStatus(String envCrn, long commandId) {
        return rangerCloudIdentityService.getRangerCloudIdentitySyncStatus(envCrn, commandId);
    }

}
