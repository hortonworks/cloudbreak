package com.sequenceiq.datalake.controller.sdx;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_CREDENTIAL;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATALAKE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.UPGRADE_DATALAKE;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN_LIST;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.validation.Valid;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.FilterParam;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.response.StackDatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SetDefaultJavaVersionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.datalake.authorization.DataLakeFiltering;
import com.sequenceiq.datalake.cm.RangerCloudIdentityService;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.SdxDeleteService;
import com.sequenceiq.datalake.service.rotation.certificate.SdxDatabaseCertificateRotationService;
import com.sequenceiq.datalake.service.sdx.SELinuxService;
import com.sequenceiq.datalake.service.sdx.SdxHorizontalScalingService;
import com.sequenceiq.datalake.service.sdx.SdxImageCatalogService;
import com.sequenceiq.datalake.service.sdx.SdxRecommendationService;
import com.sequenceiq.datalake.service.sdx.SdxRepairService;
import com.sequenceiq.datalake.service.sdx.SdxRetryService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.StorageValidationService;
import com.sequenceiq.datalake.service.sdx.VerticalScaleService;
import com.sequenceiq.datalake.service.sdx.cert.CertRenewalService;
import com.sequenceiq.datalake.service.sdx.cert.CertRotationService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.datalake.service.sdx.start.SdxStartService;
import com.sequenceiq.datalake.service.sdx.stop.SdxStopService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.AdvertisedRuntime;
import com.sequenceiq.sdx.api.model.DatalakeHorizontalScaleRequest;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncStatus;
import com.sequenceiq.sdx.api.model.SdxBackupLocationValidationRequest;
import com.sequenceiq.sdx.api.model.SdxChangeImageCatalogRequest;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseAzureRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.sdx.api.model.SdxDefaultTemplateResponse;
import com.sequenceiq.sdx.api.model.SdxGenerateImageCatalogResponse;
import com.sequenceiq.sdx.api.model.SdxInstanceMetadataUpdateRequest;
import com.sequenceiq.sdx.api.model.SdxRecommendationResponse;
import com.sequenceiq.sdx.api.model.SdxRefreshDatahubResponse;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;
import com.sequenceiq.sdx.api.model.SdxStopValidationResponse;
import com.sequenceiq.sdx.api.model.SdxSyncComponentVersionsFromCmResponse;
import com.sequenceiq.sdx.api.model.SdxValidateCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SetRangerCloudIdentityMappingRequest;
import com.sequenceiq.sdx.api.model.migraterds.SdxMigrateDatabaseV1Response;
import com.sequenceiq.sdx.api.model.rotaterdscert.SdxRotateRdsCertificateV1Response;

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
    private SdxImageCatalogService sdxImageCatalogService;

    @Inject
    private SdxRecommendationService sdxRecommendationService;

    @Inject
    private VerticalScaleService verticalScaleService;

    @Inject
    private SdxDeleteService sdxDeleteService;

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Inject
    private SdxHorizontalScalingService sdxHorizontalScalingService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private SdxDatabaseCertificateRotationService certificateRotationService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private SELinuxService seLinuxService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATALAKE)
    public SdxClusterResponse create(String name, SdxClusterRequest createSdxClusterRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        validateSdxClusterShape(createSdxClusterRequest.getClusterShape());
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (entitlementService.isSingleServerRejectEnabled(accountId)) {
            validateAzureDatabaseType(createSdxClusterRequest.getExternalDatabase());
        }
        Pair<SdxCluster, FlowIdentifier> result = sdxService.createSdx(userCrn, name, createSdxClusterRequest, null);
        SdxCluster sdxCluster = result.getLeft();
        MetricType metricType = createSdxClusterRequest.getImage() != null
                ? MetricType.CUSTOM_SDX_REQUESTED
                : MetricType.EXTERNAL_SDX_REQUESTED;
        metricService.incrementMetricCounter(metricType, sdxCluster);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        sdxClusterResponse.setName(sdxCluster.getClusterName());
        sdxClusterResponse.setFlowIdentifier(result.getRight());
        return sdxClusterResponse;
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RESIZE_DATALAKE)
    public SdxClusterResponse resize(@ResourceName String name, SdxClusterResizeRequest resizeSdxClusterRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        validateSdxClusterShape(resizeSdxClusterRequest.getClusterShape());
        Pair<SdxCluster, FlowIdentifier> result = sdxService.resizeSdx(userCrn, name, resizeSdxClusterRequest);
        SdxCluster sdxCluster = result.getLeft();
        metricService.incrementMetricCounter(MetricType.EXTERNAL_SDX_REQUESTED, sdxCluster);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        sdxClusterResponse.setName(sdxCluster.getClusterName());
        sdxClusterResponse.setFlowIdentifier(result.getRight());
        return sdxClusterResponse;
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RESIZE_DATALAKE)
    public SdxRefreshDatahubResponse refreshDataHubs(@ResourceName String name, String datahubName) {
        return sdxService.refreshDataHub(name, datahubName);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_ENVIRONMENT)
    @CheckPermissionByRequestProperty(path = "credentialCrn", type = CRN, action = DESCRIBE_CREDENTIAL)
    public ObjectStorageValidateResponse validateCloudStorage(String clusterName,
            @RequestObject SdxValidateCloudStorageRequest sdxValidateCloudStorageRequest) {
        return storageValidationService.validateObjectStorage(sdxValidateCloudStorageRequest.getCredentialCrn(),
                sdxValidateCloudStorageRequest.getSdxCloudStorageRequest(), sdxValidateCloudStorageRequest.getBlueprintName(), clusterName,
                sdxValidateCloudStorageRequest.getDataAccessRole(), sdxValidateCloudStorageRequest.getRangerAuditRole(),
                sdxValidateCloudStorageRequest.getRangerCloudAccessAuthorizerRole());
    }

    @Override
    @CheckPermissionByRequestProperty(path = "clusterName", type = NAME, action = DESCRIBE_DATALAKE)
    public ValidationResult validateBackupStorage(@RequestObject SdxBackupLocationValidationRequest sdxBackupLocationValidationRequest) {
        SdxCluster sdxCluster = getSdxClusterByName(sdxBackupLocationValidationRequest.getClusterName());
        return storageValidationService.validateBackupStorage(sdxCluster, sdxBackupLocationValidationRequest.getOperationType(),
                sdxBackupLocationValidationRequest.getBackupLocation());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_DATALAKE)
    public FlowIdentifier delete(@ResourceName String name, Boolean forced) {
        return sdxDeleteService.deleteSdx(ThreadBasedUserCrnProvider.getAccountId(), name, forced);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_DATALAKE)
    public FlowIdentifier deleteByCrn(@ResourceCrn String clusterCrn, Boolean forced) {
        return sdxDeleteService.deleteSdxByClusterCrn(ThreadBasedUserCrnProvider.getAccountId(), clusterCrn, forced);
    }

    @Override
    @CheckPermissionByResourceName(action = DESCRIBE_DATALAKE)
    public SdxClusterResponse get(@ResourceName String name) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return sdxClusterConverter.sdxClusterToResponse(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATALAKE)
    public SdxClusterResponse getByCrn(@ResourceCrn String clusterCrn) {
        SdxCluster sdxCluster = getSdxClusterByCrn(clusterCrn);
        return sdxClusterConverter.sdxClusterToResponse(sdxCluster);
    }

    @Override
    @FilterListBasedOnPermissions
    public List<SdxClusterResponse> list(@FilterParam(DataLakeFiltering.ENV_NAME) String envName, boolean includeDetached) {
        List<SdxCluster> sdxClusters = dataLakeFiltering.filterDataLakesByEnvNameOrAll(DESCRIBE_DATALAKE, envName);
        return includeDetached ? convertSdxClusters(sdxClusters) : convertAttachedSdxClusters(sdxClusters, false);
    }

    @Override
    @InternalOnly
    public List<SdxClusterResponse> internalList(@AccountId String accountId) {
        List<SdxCluster> sdxClusters = sdxService.listSdx(ThreadBasedUserCrnProvider.getUserCrn(), null);
        return convertAttachedSdxClusters(sdxClusters, false);
    }

    @Override
    @FilterListBasedOnPermissions
    public List<SdxClusterResponse> getByEnvCrn(@FilterParam(DataLakeFiltering.ENV_CRN) @ResourceCrn String envCrn, boolean includeDetached) {
        List<SdxCluster> sdxClusters = dataLakeFiltering.filterDataLakesByEnvCrn(DESCRIBE_DATALAKE, envCrn, includeDetached);
        return convertAttachedSdxClusters(sdxClusters, includeDetached);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_DETAILED_DATALAKE)
    public SdxClusterDetailResponse getDetail(@ResourceName String name, Set<String> entries) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        StackV4Response stackV4Response = sdxService.getDetail(name, entries, sdxCluster.getAccountId());
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        return SdxClusterDetailResponse.create(sdxClusterResponse, stackV4Response);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DETAILED_DATALAKE)
    public SdxClusterDetailResponse getDetailByCrn(@ResourceCrn String clusterCrn, Set<String> entries) {
        SdxCluster sdxCluster = getSdxClusterByCrn(clusterCrn);
        StackV4Response stackV4Response = sdxService.getDetail(sdxCluster.getClusterName(), entries, sdxCluster.getAccountId());
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        return SdxClusterDetailResponse.create(sdxClusterResponse, stackV4Response);
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
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REPAIR_DATALAKE)
    public FlowIdentifier renewCertificate(@ResourceCrn String crn) {
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
    public void syncByCrn(@ResourceCrn String crn) {
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
    public FlowIdentifier retryByCrn(@ResourceCrn String crn) {
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
    public FlowIdentifier startByCrn(@ResourceCrn String crn) {
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
    public FlowIdentifier stopByCrn(@ResourceCrn String crn) {
        SdxCluster sdxCluster = getSdxClusterByCrn(crn);
        return sdxStopService.triggerStopIfClusterNotStopped(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.ROTATE_SALTUSER_PASSWORD_DATALAKE)
    public FlowIdentifier rotateSaltPasswordByCrn(@ResourceCrn String crn) {
        SdxCluster sdxCluster = getSdxClusterByCrn(crn);
        return sdxService.rotateSaltPassword(sdxCluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPDATE_SALT_DATALAKE)
    public FlowIdentifier updateSaltByCrn(@ResourceCrn String crn) {
        SdxCluster sdxCluster = getSdxClusterByCrn(crn);
        return sdxService.updateSalt(sdxCluster);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATALAKE)
    public List<String> versions(String cloudPlatform, String os) {
        return cdpConfigService.getDatalakeVersions(cloudPlatform, os);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATALAKE)
    public List<AdvertisedRuntime> advertisedRuntimes(String cloudPlatform, String os, boolean armEnabled) {
        return cdpConfigService.getAdvertisedRuntimes(cloudPlatform, os, armEnabled);
    }

    @Override
    @InternalOnly
    public RangerCloudIdentitySyncStatus setRangerCloudIdentityMapping(@ResourceCrn String envCrn, SetRangerCloudIdentityMappingRequest request) {
        if (request.getAzureGroupMapping() != null) {
            throw new IllegalArgumentException("Azure group mappings is unsupported");
        }
        return rangerCloudIdentityService.setAzureCloudIdentityMapping(envCrn, request.getAzureUserMapping());
    }

    @Override
    @InternalOnly
    public RangerCloudIdentitySyncStatus getRangerCloudIdentitySyncStatus(@ResourceCrn String envCrn, long commandId) {
        return rangerCloudIdentityService.getRangerCloudIdentitySyncStatus(envCrn, List.of(commandId));
    }

    @Override
    @InternalOnly
    public RangerCloudIdentitySyncStatus getRangerCloudIdentitySyncStatus(@ResourceCrn String envCrn, List<Long> commandIds) {
        return rangerCloudIdentityService.getRangerCloudIdentitySyncStatus(envCrn, commandIds);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.START_DATALAKE)
    public void enableRangerRazByCrn(@ResourceCrn String crn) {
        sdxService.updateRangerRazEnabled(getSdxClusterByCrn(crn));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.START_DATALAKE)
    public void enableRangerRazByName(@ResourceName String name) {
        sdxService.updateRangerRazEnabled(getSdxClusterByName(name));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.ROTATE_CERT_DATALAKE)
    public FlowIdentifier rotateAutoTlsCertificatesByName(@ResourceName String name, CertificatesRotationV4Request rotateCertificateRequest) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return certRotationService.rotateAutoTlsCertificates(sdxCluster, rotateCertificateRequest);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.ROTATE_CERT_DATALAKE)
    public FlowIdentifier rotateAutoTlsCertificatesByCrn(@ResourceCrn String crn, CertificatesRotationV4Request rotateCertificateRequest) {
        SdxCluster sdxCluster = getSdxClusterByCrn(crn);
        return certRotationService.rotateAutoTlsCertificates(sdxCluster, rotateCertificateRequest);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.CHANGE_IMAGE_CATALOG_DATALAKE)
    @CheckPermissionByRequestProperty(type = NAME, path = "imageCatalog", action = DESCRIBE_IMAGE_CATALOG)
    public void changeImageCatalog(@ResourceName String name, @RequestObject SdxChangeImageCatalogRequest changeImageCatalogRequest) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        sdxImageCatalogService.changeImageCatalog(sdxCluster, changeImageCatalogRequest.getImageCatalog());
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

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATALAKE)
    public Set<String> getInstanceGroupNamesBySdxDetails(SdxClusterShape clusterShape, String runtimeVersion,
            String cloudPlatform) {
        validateSdxClusterShape(clusterShape);
        return sdxService.getInstanceGroupNamesBySdxDetails(clusterShape, runtimeVersion, cloudPlatform);
    }

    @Override
    @CheckPermissionByResourceName(action = DESCRIBE_DATALAKE)
    public SdxGenerateImageCatalogResponse generateImageCatalog(@ResourceName String name) {
        CloudbreakImageCatalogV3 imageCatalog = sdxImageCatalogService.generateImageCatalog(name);
        return new SdxGenerateImageCatalogResponse(imageCatalog);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATALAKE)
    public SdxDefaultTemplateResponse getDefaultTemplate(SdxClusterShape clusterShape, String runtimeVersion, String cloudPlatform, String architecture) {
        validateSdxClusterShape(clusterShape);
        return sdxRecommendationService.getDefaultTemplateResponse(clusterShape, runtimeVersion, cloudPlatform, Optional.ofNullable(architecture).map(
                Architecture::fromStringWithValidation).orElse(Architecture.X86_64));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATALAKE)
    @CheckPermissionByResourceCrn(action = DESCRIBE_CREDENTIAL)
    public SdxRecommendationResponse getRecommendation(@ResourceCrn String credentialCrn, SdxClusterShape clusterShape, String runtimeVersion,
            String cloudPlatform, String region, String availabilityZone, String architecture) {
        validateSdxClusterShape(clusterShape);
        return sdxRecommendationService.getRecommendation(credentialCrn, clusterShape, runtimeVersion, cloudPlatform, region, availabilityZone, architecture);
    }

    @Override
    @InternalOnly
    public SdxStopValidationResponse isStoppableInternal(@ResourceCrn String crn, @InitiatorUserCrn String initiatorUserCrn) {
        SdxCluster sdxCluster = sdxService.getByCrn(initiatorUserCrn, crn);
        Optional<String> unstoppableReason = sdxStopService.checkIfStoppable(sdxCluster);
        return new SdxStopValidationResponse(unstoppableReason.isEmpty(), unstoppableReason.orElse(null));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DATALAKE_VERTICAL_SCALING)
    public FlowIdentifier verticalScalingByCrn(@ResourceCrn String crn, StackVerticalScaleV4Request updateRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(crn);
        return verticalScaleService.verticalScaleDatalake(sdxCluster, updateRequest, userCrn);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DATALAKE_VERTICAL_SCALING)
    public FlowIdentifier verticalScalingByName(@ResourceName String name, StackVerticalScaleV4Request updateRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByNameInAccount(userCrn, name);
        return verticalScaleService.verticalScaleDatalake(sdxCluster, updateRequest, userCrn);
    }

    @Override
    @InternalOnly
    public void submitDatalakeDataSizesInternal(@ResourceCrn String crn, String operationId, String dataSizesJson,
            @InitiatorUserCrn String initiatorUserCrn) {
        sdxService.getByCrn(crn);
        sdxBackupRestoreService.submitDatalakeDataInfo(operationId, dataSizesJson, ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DATALAKE_HORIZONTAL_SCALING)
    public FlowIdentifier horizontalScaleByName(@ResourceName String name, DatalakeHorizontalScaleRequest scaleRequest) {
        return sdxHorizontalScalingService.horizontalScaleDatalake(name, scaleRequest);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DATALAKE_VERTICAL_SCALING)
    public FlowIdentifier diskUpdateByName(@ResourceName String name, DiskUpdateRequest updateRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return verticalScaleService.updateDisksDatalake(sdxCluster, updateRequest, userCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DATALAKE_VERTICAL_SCALING)
    public FlowIdentifier diskUpdateByCrn(@ResourceCrn String crn, DiskUpdateRequest updateRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = getSdxClusterByCrn(crn);
        return verticalScaleService.updateDisksDatalake(sdxCluster, updateRequest, userCrn);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DATALAKE_VERTICAL_SCALING)
    public FlowIdentifier addVolumesByStackName(@ResourceName String name, StackAddVolumesRequest addVolumesRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return verticalScaleService.addVolumesDatalake(sdxCluster, addVolumesRequest, userCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DATALAKE_VERTICAL_SCALING)
    public FlowIdentifier addVolumesByStackCrn(@ResourceCrn String crn, StackAddVolumesRequest addVolumesRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = getSdxClusterByCrn(crn);
        return verticalScaleService.addVolumesDatalake(sdxCluster, addVolumesRequest, userCrn);
    }

    @CheckPermissionByRequestProperty(action = UPGRADE_DATALAKE, type = CRN, path = "crn")
    public FlowIdentifier instanceMetadataUpdate(@RequestObject SdxInstanceMetadataUpdateRequest request) {
        return sdxReactorFlowManager.triggerInstanceMetadataUpdate(getSdxClusterByCrn(request.getCrn()), request.getUpdateType());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REPAIR_DATALAKE)
    public SdxRotateRdsCertificateV1Response rotateRdsCertificateByName(@ResourceName String name) {
        return certificateRotationService.rotateCertificate(getSdxClusterByName(name).getCrn());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REPAIR_DATALAKE)
    public SdxRotateRdsCertificateV1Response rotateRdsCertificateByCrn(@ResourceCrn String crn) {
        return certificateRotationService.rotateCertificate(crn);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "crns", type = CRN_LIST, action = DESCRIBE_DATALAKE)
    public StackDatabaseServerCertificateStatusV4Responses listDatabaseServersCertificateStatus(
            @RequestObject SdxDatabaseServerCertificateStatusV4Request request) {
        return certificateRotationService.getDatabaseCertificateStatus(request);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REPAIR_DATALAKE)
    public SdxMigrateDatabaseV1Response migrateDatabaseToSslByName(@ResourceName String name) {
        return null;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REPAIR_DATALAKE)
    public SdxMigrateDatabaseV1Response migrateDatabaseToSslByCrn(@ResourceCrn String crn) {
        return null;
    }

    @Override
    @CheckPermissionByResourceName(action = UPGRADE_DATALAKE)
    public FlowIdentifier setDefaultJavaVersionByName(@ResourceName String name, SetDefaultJavaVersionRequest request) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        sdxService.validateDefaultJavaVersionUpdate(sdxCluster.getCrn(), request);
        return sdxReactorFlowManager.triggerSetDefaultJavaVersion(sdxCluster, request.getDefaultJavaVersion(), request.isRestartServices(),
                request.isRestartCM(), request.isRollingRestart());
    }

    @Override
    @CheckPermissionByResourceCrn(action = UPGRADE_DATALAKE)
    public FlowIdentifier setDefaultJavaVersionByCrn(@ResourceCrn String crn, SetDefaultJavaVersionRequest request) {
        SdxCluster sdxCluster = getSdxClusterByCrn(crn);
        sdxService.validateDefaultJavaVersionUpdate(crn, request);
        return sdxReactorFlowManager.triggerSetDefaultJavaVersion(sdxCluster, request.getDefaultJavaVersion(), request.isRestartServices(),
                request.isRestartCM(), request.isRollingRestart());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DATALAKE_VERTICAL_SCALING)
    public FlowIdentifier updateRootVolumeByDatalakeName(@ResourceName String name, DiskUpdateRequest updateRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return verticalScaleService.updateRootVolumeDatalake(sdxCluster, updateRequest, userCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DATALAKE_VERTICAL_SCALING)
    public FlowIdentifier updateRootVolumeByDatalakeCrn(@ResourceCrn String crn, DiskUpdateRequest updateRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = getSdxClusterByCrn(crn);
        return verticalScaleService.updateRootVolumeDatalake(sdxCluster, updateRequest, userCrn);
    }

    @Override
    @CheckPermissionByResourceName(action =  AuthorizationResourceAction.REPAIR_DATALAKE)
    public FlowIdentifier modifySeLinuxByName(@ResourceName String name, SeLinux selinuxMode) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return seLinuxService.modifySeLinuxOnDatalake(sdxCluster, userCrn, selinuxMode);
    }

    @Override
    @CheckPermissionByResourceCrn(action =  AuthorizationResourceAction.REPAIR_DATALAKE)
    public FlowIdentifier modifySeLinuxByCrn(@ResourceCrn String crn, SeLinux selinuxMode) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = getSdxClusterByCrn(crn);
        return seLinuxService.modifySeLinuxOnDatalake(sdxCluster, userCrn, selinuxMode);
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

    private List<SdxClusterResponse> convertAttachedSdxClusters(List<SdxCluster> sdxClusters, boolean includeDetached) {
        return sdxClusters.stream()
                .filter(sdx -> includeDetached || !sdx.isDetached())
                .map(sdx -> sdxClusterConverter.sdxClusterToResponse(sdx))
                .collect(Collectors.toList());
    }

    private List<SdxClusterResponse> convertSdxClusters(List<SdxCluster> sdxClusters) {
        return sdxClusters.stream()
                .map(sdx -> sdxClusterConverter.sdxClusterToResponse(sdx))
                .collect(Collectors.toList());
    }

    private void validateSdxClusterShape(SdxClusterShape shape) {
        if (SdxClusterShape.CONTAINERIZED == shape) {
            throw new BadRequestException("CONTAINERIZED shape is not acceptable for this request. The CONTAINERIZED shape is not supported." +
                " Please confirm, your cluster shape and retry the operation.");
        }
    }

    private void validateAzureDatabaseType(@Valid SdxDatabaseRequest sdxDatabaseRequest) {
        Optional.ofNullable(sdxDatabaseRequest)
                .map(SdxDatabaseRequest::getSdxDatabaseAzureRequest)
                .map(SdxDatabaseAzureRequest::getAzureDatabaseType)
                .ifPresent(azureDatabaseType -> {
                    if (azureDatabaseType == AzureDatabaseType.SINGLE_SERVER) {
                        throw new BadRequestException("Azure Database for PostgreSQL - Single Server is retired. New deployments cannot be created anymore. " +
                                "Check documentation for more information: " +
                                "https://learn.microsoft.com/en-us/azure/postgresql/migrate/whats-happening-to-postgresql-single-server");
                    }
        });
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_DETAILED_DATALAKE)
    public SdxClusterDetailResponse getSdxDetailWithResourcesByName(@ResourceName String name, Set<String> entries) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        StackV4Response stackV4Response = sdxService.getDetailWithResources(name, entries, sdxCluster.getAccountId());
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        return SdxClusterDetailResponse.create(sdxClusterResponse, stackV4Response);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REPAIR_DATALAKE)
    public FlowIdentifier triggerSkuMigrationByName(@ResourceName String name, boolean force) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        sdxService.validateSkuMigration(sdxCluster);
        return sdxReactorFlowManager.triggerSkuMigration(sdxCluster, force);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REPAIR_DATALAKE)
    public FlowIdentifier triggerSkuMigrationByCrn(@ResourceCrn String crn, boolean force) {
        SdxCluster sdxCluster = getSdxClusterByCrn(crn);
        sdxService.validateSkuMigration(sdxCluster);
        return sdxReactorFlowManager.triggerSkuMigration(sdxCluster, force);
    }
}
