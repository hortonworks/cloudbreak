package com.sequenceiq.datalake.controller.sdx;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_CREDENTIAL;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.FilterParam;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.cloudbreak.validation.ValidStackNameFormat;
import com.sequenceiq.cloudbreak.validation.ValidStackNameLength;
import com.sequenceiq.datalake.authorization.DataLakeFiltering;
import com.sequenceiq.datalake.cm.RangerCloudIdentityService;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.sdx.SdxImageCatalogChangeService;
import com.sequenceiq.datalake.service.sdx.SdxRepairService;
import com.sequenceiq.datalake.service.sdx.SdxRetryService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.StorageValidationService;
import com.sequenceiq.datalake.service.sdx.cert.CertRenewalService;
import com.sequenceiq.datalake.service.sdx.cert.CertRotationService;
import com.sequenceiq.datalake.service.sdx.start.SdxStartService;
import com.sequenceiq.datalake.service.sdx.stop.SdxStopService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.AdvertisedRuntime;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncStatus;
import com.sequenceiq.sdx.api.model.SdxChangeImageCatalogRequest;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxCustomClusterRequest;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;
import com.sequenceiq.sdx.api.model.SdxSyncComponentVersionsFromCmResponse;
import com.sequenceiq.sdx.api.model.SdxValidateCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SetRangerCloudIdentityMappingRequest;

@Controller
@AccountEntityType(SdxCluster.class)
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
    private RangerCloudIdentityService rangerCloudIdentityService;

    @Inject
    private CertRotationService certRotationService;

    @Inject
    private CertRenewalService certRenewalService;

    @Inject
    private DataLakeFiltering dataLakeFiltering;

    @Inject
    private StorageValidationService storageValidationService;

    @Inject
    private SdxImageCatalogChangeService sdxImageCatalogChangeService;

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
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATALAKE)
    public SdxClusterResponse create(String name, @Valid SdxCustomClusterRequest createSdxClusterRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        Pair<SdxCluster, FlowIdentifier> result = sdxService.createSdx(userCrn, name, createSdxClusterRequest);
        SdxCluster sdxCluster = result.getLeft();
        metricService.incrementMetricCounter(MetricType.CUSTOM_SDX_REQUESTED, sdxCluster);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        sdxClusterResponse.setName(sdxCluster.getClusterName());
        sdxClusterResponse.setFlowIdentifier(result.getRight());
        return sdxClusterResponse;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATALAKE)
    public SdxClusterResponse resize(String name, SdxClusterResizeRequest resizeSdxClusterRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        Pair<SdxCluster, FlowIdentifier> result = sdxService.resizeSdx(userCrn, name, resizeSdxClusterRequest);
        SdxCluster sdxCluster = result.getLeft();
        metricService.incrementMetricCounter(MetricType.EXTERNAL_SDX_REQUESTED, sdxCluster);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        sdxClusterResponse.setName(sdxCluster.getClusterName());
        sdxClusterResponse.setFlowIdentifier(result.getRight());
        return sdxClusterResponse;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_ENVIRONMENT)
    @CheckPermissionByRequestProperty(path = "credentialCrn", type = CRN, action = DESCRIBE_CREDENTIAL)
    public ObjectStorageValidateResponse validateCloudStorage(@ValidStackNameFormat @ValidStackNameLength String clusterName,
            @RequestObject @Valid SdxValidateCloudStorageRequest sdxValidateCloudStorageRequest) {
        return storageValidationService.validateObjectStorage(sdxValidateCloudStorageRequest.getCredentialCrn(),
                sdxValidateCloudStorageRequest.getSdxCloudStorageRequest(), sdxValidateCloudStorageRequest.getBlueprintName(), clusterName,
                sdxValidateCloudStorageRequest.getDataAccessRole(), sdxValidateCloudStorageRequest.getRangerAuditRole());
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
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return sdxClusterConverter.sdxClusterToResponse(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATALAKE)
    public SdxClusterResponse getByCrn(@TenantAwareParam @ResourceCrn String clusterCrn) {
        SdxCluster sdxCluster = getSdxClusterByCrn(clusterCrn);
        return sdxClusterConverter.sdxClusterToResponse(sdxCluster);
    }

    @Override
    @FilterListBasedOnPermissions
    public List<SdxClusterResponse> list(@FilterParam(DataLakeFiltering.ENV_NAME) String envName, boolean includeDetached) {
        List<SdxCluster> sdxClusters = dataLakeFiltering.filterDataLakesByEnvNameOrAll(AuthorizationResourceAction.DESCRIBE_DATALAKE, envName);
        return includeDetached ? convertSdxClusters(sdxClusters) : convertAttachedSdxClusters(sdxClusters);
    }

    @Override
    @InternalOnly
    public List<SdxClusterResponse> internalList(@AccountId String accountId) {
        List<SdxCluster> sdxClusters = sdxService.listSdx(ThreadBasedUserCrnProvider.getUserCrn(), null);
        return convertAttachedSdxClusters(sdxClusters);
    }

    @Override
    @FilterListBasedOnPermissions
    public List<SdxClusterResponse> getByEnvCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @FilterParam(DataLakeFiltering.ENV_CRN)
    @TenantAwareParam String envCrn) {
        List<SdxCluster> sdxClusters = dataLakeFiltering.filterDataLakesByEnvCrn(AuthorizationResourceAction.DESCRIBE_DATALAKE, envCrn);
        return convertAttachedSdxClusters(sdxClusters);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_DETAILED_DATALAKE)
    public SdxClusterDetailResponse getDetail(@ResourceName String name, Set<String> entries) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        StackV4Response stackV4Response = sdxService.getDetail(name, entries, sdxCluster.getAccountId());
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        return new SdxClusterDetailResponse(sdxClusterResponse, stackV4Response);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DETAILED_DATALAKE)
    public SdxClusterDetailResponse getDetailByCrn(@TenantAwareParam @ResourceCrn String clusterCrn, Set<String> entries) {
        SdxCluster sdxCluster = getSdxClusterByCrn(clusterCrn);
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
    public FlowIdentifier repairClusterByCrn(@ResourceCrn @TenantAwareParam String clusterCrn, SdxRepairRequest clusterRepairRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return repairService.triggerRepairByCrn(userCrn, clusterCrn, clusterRepairRequest);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REPAIR_DATALAKE)
    public FlowIdentifier renewCertificate(@ResourceCrn @TenantAwareParam String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        return certRenewalService.triggerRenewCertificate(sdxCluster, userCrn);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SYNC_DATALAKE)
    public void sync(@ResourceName String name) {
        sdxService.sync(name, ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SYNC_DATALAKE)
    public void syncByCrn(@ResourceCrn @TenantAwareParam String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        sdxService.syncByCrn(userCrn, crn);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RETRY_DATALAKE_OPERATION)
    public FlowIdentifier retry(@ResourceName String name) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return sdxRetryService.retrySdx(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.RETRY_DATALAKE_OPERATION)
    public FlowIdentifier retryByCrn(@ResourceCrn @TenantAwareParam String crn) {
        SdxCluster sdxCluster = getSdxClusterByCrn(crn);
        return sdxRetryService.retrySdx(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.START_DATALAKE)
    public FlowIdentifier startByName(@ResourceName String name) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return sdxStartService.triggerStartIfClusterNotRunning(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.START_DATALAKE)
    public FlowIdentifier startByCrn(@ResourceCrn @TenantAwareParam String crn) {
        SdxCluster sdxCluster = getSdxClusterByCrn(crn);
        return sdxStartService.triggerStartIfClusterNotRunning(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.STOP_DATALAKE)
    public FlowIdentifier stopByName(@ResourceName String name) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return sdxStopService.triggerStopIfClusterNotStopped(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.STOP_DATALAKE)
    public FlowIdentifier stopByCrn(@ResourceCrn @TenantAwareParam String crn) {
        SdxCluster sdxCluster = getSdxClusterByCrn(crn);
        return sdxStopService.triggerStopIfClusterNotStopped(sdxCluster);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATALAKE)
    public List<String> versions(String cloudPlatform) {
        return cdpConfigService.getDatalakeVersions(cloudPlatform);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATALAKE)
    public List<AdvertisedRuntime> advertisedRuntimes(String cloudPlatform) {
        return cdpConfigService.getAdvertisedRuntimes(cloudPlatform);
    }

    @Override
    @InternalOnly
    public RangerCloudIdentitySyncStatus setRangerCloudIdentityMapping(@TenantAwareParam String envCrn, SetRangerCloudIdentityMappingRequest request) {
        if (request.getAzureGroupMapping() != null) {
            throw new IllegalArgumentException("Azure group mappings is unsupported");
        }
        return rangerCloudIdentityService.setAzureCloudIdentityMapping(envCrn, request.getAzureUserMapping());
    }

    @Override
    @InternalOnly
    public RangerCloudIdentitySyncStatus getRangerCloudIdentitySyncStatus(@TenantAwareParam String envCrn, long commandId) {
        return rangerCloudIdentityService.getRangerCloudIdentitySyncStatus(envCrn, commandId);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.ROTATE_CERT_DATALAKE)
    public FlowIdentifier rotateAutoTlsCertificatesByName(@ResourceName String name, @Valid CertificatesRotationV4Request rotateCertificateRequest) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return certRotationService.rotateAutoTlsCertificates(sdxCluster, rotateCertificateRequest);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.ROTATE_CERT_DATALAKE)
    public FlowIdentifier rotateAutoTlsCertificatesByCrn(@ResourceCrn @TenantAwareParam String crn,
            @Valid CertificatesRotationV4Request rotateCertificateRequest) {
        SdxCluster sdxCluster = getSdxClusterByCrn(crn);
        return certRotationService.rotateAutoTlsCertificates(sdxCluster, rotateCertificateRequest);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.CHANGE_IMAGE_CATALOG_DATALAKE)
    public void changeImageCatalog(@ResourceName String name, SdxChangeImageCatalogRequest changeImageCatalogRequest) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        sdxImageCatalogChangeService.changeImageCatalog(sdxCluster, changeImageCatalogRequest.getImageCatalog());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SYNC_COMPONENT_VERSIONS_FROM_CM_DATALAKE)
    public SdxSyncComponentVersionsFromCmResponse syncComponentVersionsFromCmByName(@ResourceName String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return new SdxSyncComponentVersionsFromCmResponse(sdxService.syncComponentVersionsFromCm(userCrn, NameOrCrn.ofName(name)));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SYNC_COMPONENT_VERSIONS_FROM_CM_DATALAKE)
    public SdxSyncComponentVersionsFromCmResponse syncComponentVersionsFromCmByCrn(@ResourceCrn String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return new SdxSyncComponentVersionsFromCmResponse(sdxService.syncComponentVersionsFromCm(userCrn, NameOrCrn.ofCrn(crn)));
    }

    private SdxCluster getSdxClusterByName(String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByNameInAccount(userCrn, name);
        MDCBuilder.buildMdcContext(sdxCluster);
        return sdxCluster;
    }

    private SdxCluster getSdxClusterByCrn(String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        MDCBuilder.buildMdcContext(sdxCluster);
        return sdxCluster;
    }

    private List<SdxClusterResponse> convertAttachedSdxClusters(List<SdxCluster> sdxClusters) {
        // Filters out detached clusters.
        return sdxClusters.stream()
                .filter(sdx -> !sdx.isDetached())
                .map(sdx -> sdxClusterConverter.sdxClusterToResponse(sdx))
                .collect(Collectors.toList());
    }

    private List<SdxClusterResponse> convertSdxClusters(List<SdxCluster> sdxClusters) {
        return sdxClusters.stream()
                .map(sdx -> sdxClusterConverter.sdxClusterToResponse(sdx))
                .collect(Collectors.toList());
    }

}
