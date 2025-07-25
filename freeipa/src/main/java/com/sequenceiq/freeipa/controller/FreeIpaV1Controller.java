package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_RECIPE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.EDIT_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.ENVIRONMENT_CHANGE_FREEIPA_IMAGE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.REPAIR_FREEIPA;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.SCALE_FREEIPA;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME_LIST;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.State;
import com.sequenceiq.common.api.UsedSubnetWithResourceResponse;
import com.sequenceiq.common.api.UsedSubnetsByEnvironmentResponse;
import com.sequenceiq.common.api.type.OutboundType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.common.model.SubnetIdWithResourceNameAndCrn;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.RetryableFlowResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.ModifySeLinuxResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.binduser.BindUserCreateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageChangeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.FreeIpaRecommendationResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.imagecatalog.ChangeImageCatalogRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.imagecatalog.GenerateImageCatalogResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.imdupdate.InstanceMetadataUpdateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.reboot.RebootInstancesRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rebuild.RebuildRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.repair.RepairInstancesRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DiskUpdateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpdateRootVolumeResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpscaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpscaleResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleResponse;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.authorization.FreeIpaFiltering;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionWrapper;
import com.sequenceiq.freeipa.controller.validation.AttachChildEnvironmentRequestValidator;
import com.sequenceiq.freeipa.controller.validation.CreateFreeIpaRequestValidator;
import com.sequenceiq.freeipa.dto.RotateSaltPasswordReason;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.RotateSaltPasswordService;
import com.sequenceiq.freeipa.orchestrator.SaltUpdateService;
import com.sequenceiq.freeipa.service.binduser.BindUserCreateService;
import com.sequenceiq.freeipa.service.freeipa.cert.root.FreeIpaRootCertificateService;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;
import com.sequenceiq.freeipa.service.image.ImageCatalogChangeService;
import com.sequenceiq.freeipa.service.image.ImageCatalogGeneratorService;
import com.sequenceiq.freeipa.service.image.ImageChangeService;
import com.sequenceiq.freeipa.service.operation.FreeIpaRetryService;
import com.sequenceiq.freeipa.service.stack.ChildEnvironmentService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.FreeIpaCreationService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDeletionService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDescribeService;
import com.sequenceiq.freeipa.service.stack.FreeIpaListService;
import com.sequenceiq.freeipa.service.stack.FreeIpaRecommendationService;
import com.sequenceiq.freeipa.service.stack.FreeIpaScalingService;
import com.sequenceiq.freeipa.service.stack.FreeIpaStackHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.FreeIpaStartService;
import com.sequenceiq.freeipa.service.stack.FreeIpaStopService;
import com.sequenceiq.freeipa.service.stack.FreeIpaUpgradeCcmService;
import com.sequenceiq.freeipa.service.stack.FreeIpaUpgradeDefaultOutboundService;
import com.sequenceiq.freeipa.service.stack.FreeipaInstanceMetadataUpdateService;
import com.sequenceiq.freeipa.service.stack.FreeipaModifyProxyConfigService;
import com.sequenceiq.freeipa.service.stack.RepairInstancesService;
import com.sequenceiq.freeipa.service.stack.RootVolumeUpdateService;
import com.sequenceiq.freeipa.service.stack.SeLinuxModificationService;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
@AccountEntityType(Stack.class)
public class FreeIpaV1Controller implements FreeIpaV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaV1Controller.class);

    @Inject
    private FreeIpaCreationService freeIpaCreationService;

    @Inject
    private ChildEnvironmentService childEnvironmentService;

    @Inject
    private FreeIpaDeletionService freeIpaDeletionService;

    @Inject
    private FreeIpaDescribeService freeIpaDescribeService;

    @Inject
    private FreeIpaListService freeIpaListService;

    @Inject
    private FreeIpaStackHealthDetailsService freeIpaStackHealthDetailsService;

    @Inject
    private FreeIpaRootCertificateService freeIpaRootCertificateService;

    @Inject
    private CleanupService cleanupService;

    @Inject
    private RepairInstancesService repairInstancesService;

    @Inject
    private FreeIpaScalingService freeIpaScalingService;

    @Inject
    private CrnService crnService;

    @Inject
    private CreateFreeIpaRequestValidator createFreeIpaRequestValidator;

    @Inject
    private AttachChildEnvironmentRequestValidator attachChildEnvironmentRequestValidator;

    @Inject
    private FreeIpaStartService freeIpaStartService;

    @Inject
    private FreeIpaStopService freeIpaStopService;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private FreeIpaFiltering freeIpaFiltering;

    @Inject
    private BindUserCreateService bindUserCreateService;

    @Inject
    private ImageChangeService imageChangeService;

    @Inject
    private SaltUpdateService saltUpdateService;

    @Inject
    private RotateSaltPasswordService rotateSaltPasswordService;

    @Inject
    private FreeIpaRetryService retryService;

    @Inject
    private ImageCatalogChangeService imageCatalogChangeService;

    @Inject
    private ImageCatalogGeneratorService imageCatalogGeneratorService;

    @Inject
    private FreeIpaUpgradeCcmService upgradeCcmService;

    @Inject
    private FreeIpaUpgradeDefaultOutboundService upgradeDefaultOutboundService;

    @Inject
    private FreeipaModifyProxyConfigService modifyProxyConfigService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private FreeIpaRecommendationService freeIpaRecommendationService;

    @Inject
    private FreeipaInstanceMetadataUpdateService instanceMetadataUpdateService;

    @Inject
    private RootVolumeUpdateService rootVolumeUpdateService;

    @Inject
    private SeLinuxModificationService seLinuxModificationService;

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    @CheckPermissionByRequestProperty(path = "recipes", type = NAME_LIST, action = DESCRIBE_RECIPE, skipOnNull = true)
    public DescribeFreeIpaResponse create(@RequestObject CreateFreeIpaRequest request) {
        ValidationResult validationResult = createFreeIpaRequestValidator.validate(request);
        if (validationResult.getState() == State.ERROR) {
            LOGGER.debug("FreeIPA request has validation error(s): {}.", validationResult.getFormattedErrors());
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        String accountId = crnService.getCurrentAccountId();
        return freeIpaCreationService.launchFreeIpa(request, accountId);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "parentEnvironmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    @CheckPermissionByRequestProperty(path = "childEnvironmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public void attachChildEnvironment(@RequestObject AttachChildEnvironmentRequest request) {
        ValidationResult validationResult = attachChildEnvironmentRequestValidator.validate(request);
        if (validationResult.hasError()) {
            LOGGER.debug("AttachChildEnvironmentRequest has validation error(s): {}.", validationResult.getFormattedErrors());
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        String accountId = crnService.getCurrentAccountId();
        childEnvironmentService.attachChildEnvironment(request, accountId);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "parentEnvironmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    @CheckPermissionByRequestProperty(path = "childEnvironmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public void detachChildEnvironment(@RequestObject DetachChildEnvironmentRequest request) {
        String accountId = crnService.getCurrentAccountId();
        childEnvironmentService.detachChildEnvironment(request, accountId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public DescribeFreeIpaResponse describe(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaDescribeService.describe(environmentCrn, accountId);
    }

    @Override
    @InternalOnly
    public DescribeFreeIpaResponse describeInternal(@ResourceCrn String environmentCrn, @AccountId String accountId) {
        return freeIpaDescribeService.describe(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public List<DescribeFreeIpaResponse> describeAll(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaDescribeService.describeAll(environmentCrn, accountId);
    }

    @Override
    @InternalOnly
    public List<DescribeFreeIpaResponse> describeAllInternal(@ResourceCrn String environmentCrn, @AccountId String accountId) {
        return freeIpaDescribeService.describeAll(environmentCrn, accountId);
    }

    @Override
    @FilterListBasedOnPermissions
    public List<ListFreeIpaResponse> list() {
        return freeIpaFiltering.filterFreeIpas(AuthorizationResourceAction.DESCRIBE_ENVIRONMENT);
    }

    @Override
    @InternalOnly
    public List<ListFreeIpaResponse> listInternal(@AccountId String accountId) {
        return freeIpaListService.list(accountId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public HealthDetailsFreeIpaResponse healthDetails(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaStackHealthDetailsService.getHealthDetails(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public String getRootCertificate(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        try {
            return freeIpaRootCertificateService.getRootCertificate(environmentCrn, accountId);
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    @Override
    @InternalOnly
    public String getRootCertificateInternal(@ResourceCrn String environmentCrn, @AccountId String accountId) {
        try {
            return freeIpaRootCertificateService.getRootCertificate(environmentCrn, accountId);
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_ENVIRONMENT)
    public void delete(@ResourceCrn String environmentCrn, boolean forced) {
        String accountId = crnService.getCurrentAccountId();
        freeIpaDeletionService.delete(environmentCrn, accountId, forced);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public OperationStatus cleanup(@RequestObject CleanupRequest request) {
        String accountId = crnService.getCurrentAccountId();
        return internalCleanup(request, accountId);
    }

    @Override
    @InternalOnly
    public OperationStatus internalCleanup(CleanupRequest request, @AccountId String accountId) {
        return cleanupService.cleanup(accountId, request);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = REPAIR_FREEIPA)
    public OperationStatus rebootInstances(@RequestObject RebootInstancesRequest request) {
        String accountId = crnService.getCurrentAccountId();
        return repairInstancesService.rebootInstances(accountId, request);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = REPAIR_FREEIPA)
    public OperationStatus repairInstances(@RequestObject RepairInstancesRequest request) {
        String accountId = crnService.getCurrentAccountId();
        return repairInstancesService.repairInstances(accountId, request);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = REPAIR_FREEIPA)
    public DescribeFreeIpaResponse rebuild(@RequestObject RebuildRequest request) {
        String accountId = crnService.getCurrentAccountId();
        return repairInstancesService.rebuild(accountId, request);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = SCALE_FREEIPA)
    public UpscaleResponse upscale(@RequestObject UpscaleRequest request) {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaScalingService.upscale(accountId, request);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = SCALE_FREEIPA)
    public DownscaleResponse downscale(@RequestObject DownscaleRequest request) {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaScalingService.downscale(accountId, request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.START_ENVIRONMENT)
    public void start(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        freeIpaStartService.start(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.STOP_ENVIRONMENT)
    public void stop(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        freeIpaStopService.stop(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.ROTATE_SALTUSER_PASSWORD_ENVIRONMENT)
    public FlowIdentifier rotateSaltPassword(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return rotateSaltPasswordService.triggerRotateSaltPassword(environmentCrn, accountId, RotateSaltPasswordReason.MANUAL);
    }

    @Override
    @CheckPermissionByResourceCrn(action = EDIT_ENVIRONMENT)
    public String registerWithClusterProxy(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return clusterProxyService.registerFreeIpa(accountId, environmentCrn).toString();
    }

    @Override
    @CheckPermissionByResourceCrn(action = EDIT_ENVIRONMENT)
    public void deregisterWithClusterProxy(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        clusterProxyService.deregisterFreeIpa(accountId, environmentCrn);
    }

    @Override
    @InternalOnly
    public OperationStatus createBindUser(BindUserCreateRequest request, @InitiatorUserCrn String initiatorUserCrn) {
        String accountId = crnService.getCurrentAccountId();
        return bindUserCreateService.createBindUser(accountId, request);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public OperationStatus createE2ETestBindUser(@RequestObject BindUserCreateRequest request, @InitiatorUserCrn String initiatorUserCrn) {
        String accountId = crnService.getCurrentAccountId();
        if (entitlementService.isE2ETestOnlyEnabled(accountId)) {
            return bindUserCreateService.createBindUser(accountId, request);
        } else {
            throw new BadRequestException("E2e test only endpoint!");
        }
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public FlowIdentifier changeImage(@RequestObject ImageChangeRequest request) {
        String accountId = crnService.getCurrentAccountId();
        return imageChangeService.changeImage(accountId, request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = EDIT_ENVIRONMENT)
    public FlowIdentifier updateSaltByName(@ResourceCrn String environmentCrn, @AccountId String accountId) {
        String currentAccountId = Optional.ofNullable(accountId).orElseGet(ThreadBasedUserCrnProvider::getAccountId);
        return saltUpdateService.updateSaltStates(environmentCrn, currentAccountId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = EDIT_ENVIRONMENT)
    public FlowIdentifier retry(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return retryService.retry(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = EDIT_ENVIRONMENT)
    public List<RetryableFlowResponse> listRetryableFlows(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return retryService.getRetryableFlows(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = ENVIRONMENT_CHANGE_FREEIPA_IMAGE)
    public void changeImageCatalog(@ResourceCrn String environmentCrn, ChangeImageCatalogRequest changeImageCatalogRequest) {
        String accountId = crnService.getCurrentAccountId();
        imageCatalogChangeService.changeImageCatalog(environmentCrn, accountId, changeImageCatalogRequest.getImageCatalog());
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public GenerateImageCatalogResponse generateImageCatalog(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return imageCatalogGeneratorService.generate(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public Image getImage(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaDescribeService.getImage(environmentCrn, accountId);
    }

    @Override
    @InternalOnly
    public OperationStatus upgradeCcmInternal(@ResourceCrn String environmentCrn, @InitiatorUserCrn String initiatorUserCrn) {
        String accountId = crnService.getCurrentAccountId();
        return upgradeCcmService.upgradeCcm(environmentCrn, accountId);
    }

    @Override
    @InternalOnly
    public OutboundType getOutboundType(@ResourceCrn String environmentCrn, @InitiatorUserCrn String initiatorUserCrn) {
        String accountId = crnService.getCurrentAccountId();
        return upgradeDefaultOutboundService.getCurrentDefaultOutbound(environmentCrn, accountId);
    }

    @Override
    @InternalOnly
    public OperationStatus modifyProxyConfigInternal(@ResourceCrn String environmentCrn, String previousProxyCrn,
            @InitiatorUserCrn String initiatorUserCrn) {
        String accountId = crnService.getCurrentAccountId();
        return modifyProxyConfigService.modifyProxyConfig(environmentCrn, previousProxyCrn, accountId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_ENVIRONMENT)
    public FreeIpaRecommendationResponse getRecommendation(String credentialCrn, String region, String availabilityZone, String architecture) {
        return freeIpaRecommendationService.getRecommendation(credentialCrn, region, availabilityZone, architecture);
    }

    @Override
    @InternalOnly
    public VerticalScaleResponse verticalScalingByCrn(@ResourceCrn String environmentCrn, @RequestObject VerticalScaleRequest updateRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return freeIpaScalingService.verticalScale(accountId, environmentCrn, updateRequest);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public FlowIdentifier instanceMetadataUpdate(@RequestObject InstanceMetadataUpdateRequest request) {
        return instanceMetadataUpdateService.updateInstanceMetadata(request.getEnvironmentCrn(), request.getUpdateType());
    }

    @Override
    @InternalOnly
    public UsedSubnetsByEnvironmentResponse getUsedSubnetsByEnvironment(@ResourceCrn String environmentCrn) {
        List<SubnetIdWithResourceNameAndCrn> allUsedSubnets = freeIpaListService.getAllUsedSubnetsByEnvironmentCrn(environmentCrn);
        return new UsedSubnetsByEnvironmentResponse(allUsedSubnets
                .stream().map(s -> new UsedSubnetWithResourceResponse(s.getName(), s.getSubnetId(), s.getResourceCrn(), s.getType()))
                .collect(Collectors.toList()));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.ENVIRONMENT_VERTICAL_SCALING)
    public UpdateRootVolumeResponse updateRootVolumeByCrn(@ResourceCrn String environmentCrn, DiskUpdateRequest rootDiskVolumesRequest) {
        return rootVolumeUpdateService.updateRootVolume(environmentCrn, rootDiskVolumesRequest, ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public ModifySeLinuxResponse modifySelinuxByCrn(@ResourceCrn String environmentCrn, SeLinux selinuxMode) {
        if (selinuxMode.equals(SeLinux.DISABLED)) {
            throw new BadRequestException("Cannot set SELinux mode value to DISABLED.");
        }
        return seLinuxModificationService.modifySeLinuxByCrn(environmentCrn, ThreadBasedUserCrnProvider.getAccountId(), selinuxMode);
    }
}
