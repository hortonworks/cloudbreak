package com.sequenceiq.cloudbreak.service;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.common.StackEndpoint;
import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.api.model.CertificateResponse;
import com.sequenceiq.cloudbreak.api.model.GeneratedBlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackImageChangeRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackScaleRequestV2;
import com.sequenceiq.cloudbreak.api.model.stack.StackValidationRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRepairRequest;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.authorization.PermissionCheckingUtils;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.CentralBlueprintUpdater;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.controller.StackCreatorService;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.controller.validation.stack.StackRequestValidator;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidationException;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidator;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.decorator.StackDecorator;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class StackCommonService implements StackEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCommonService.class);

    @Inject
    private StackService stackService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private StackDecorator stackDecorator;

    @Inject
    private AccountPreferencesValidator accountPreferencesValidator;

    @Inject
    private CloudParameterService parameterService;

    @Inject
    private FileSystemValidator fileSystemValidator;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private StackRequestValidator stackValidator;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private ClusterCreationSetupService clusterCreationService;

    @Inject
    private StackCreatorService stackCreatorService;

    @Inject
    private ScalingHardLimitsService scalingHardLimitsService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Inject
    private CloudParameterCache cloudParameterCache;

    @Inject
    private ClusterCommonService clusterCommonService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private OperationRetryService operationRetryService;

    @Inject
    private CentralBlueprintUpdater centralBlueprintUpdater;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    public StackResponse createInOrganization(StackRequest stackRequest, Long organizationId) {
        Organization organization = organizationService.get(organizationId);
        return stackCreatorService.createStack(authenticatedUserService.getCbUser(), stackRequest, organization);
    }

    public StackResponse createInDefaultOrganization(StackRequest stackRequest) {
        return stackCreatorService.createStackInDefaultOrg(authenticatedUserService.getCbUser(), stackRequest);
    }

    public void deleteInOrganization(String name, Long organizationId, Boolean forced, Boolean deleteDependencies) {
        stackService.delete(name, organizationId, forced, deleteDependencies);
    }

    @Override
    public Set<StackResponse> getStacksInDefaultOrg() {
        return stackService.retrieveStacksForDefaultOrganization();
    }

    @Override
    public StackResponse get(Long id, Set<String> entries) {
        return stackService.getJsonById(id, entries);
    }

    @Override
    public StackResponse getStackFromDefaultOrg(String name, Set<String> entries) {
        return stackService.getStackByNameInDefaultOrg(name, entries);
    }

    @Override
    public Map<String, Object> status(Long id) {
        return conversionService.convert(stackService.getById(id), Map.class);
    }

    @Override
    public void deleteById(Long id, Boolean forced, Boolean deleteDependencies) {
        stackService.delete(id, forced, deleteDependencies);
    }

    @Override
    public void deleteInDefaultOrg(String name, Boolean forced, Boolean deleteDependencies) {
        Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
        stackService.delete(name, organization.getId(), forced, deleteDependencies);
    }

    public Response putInDefaultOrg(Long id, UpdateStackJson updateRequest) {
        Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(organization.getId(), OrganizationResource.STACK, Action.WRITE);
        Stack stack = stackService.getById(id);
        MDCBuilder.buildMdcContext(stack);
        return put(stack, updateRequest);
    }

    public Response putStopInOrganization(String name, Long organizationId) {
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(organizationId, OrganizationResource.STACK, Action.WRITE);
        Stack stack = stackService.getByNameInOrg(name, organizationId);
        MDCBuilder.buildMdcContext(stack);
        if (!cloudParameterCache.isStartStopSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Stop is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setStatus(StatusRequest.STOPPED);
        updateStackJson.setWithClusterEvent(true);
        return put(stack, updateStackJson);
    }

    public Response putSyncInOrganization(String name, Long organizationId) {
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(organizationId, OrganizationResource.STACK, Action.WRITE);
        Stack stack = stackService.getByNameInOrg(name, organizationId);
        MDCBuilder.buildMdcContext(stack);
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setStatus(StatusRequest.FULL_SYNC);
        updateStackJson.setWithClusterEvent(true);
        return put(stack, updateStackJson);
    }

    public Response putStartInOrganization(String name, Long organizationId) {
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(organizationId, OrganizationResource.STACK, Action.WRITE);
        Stack stack = stackService.getByNameInOrg(name, organizationId);
        MDCBuilder.buildMdcContext(stack);
        if (!cloudParameterCache.isStartStopSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Start is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setStatus(StatusRequest.STARTED);
        updateStackJson.setWithClusterEvent(true);
        return put(stack, updateStackJson);
    }

    public Response putScalingInOrganization(String name, Long organizationId, StackScaleRequestV2 updateRequest) {
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(organizationId, OrganizationResource.STACK, Action.WRITE);
        Stack stack = stackService.getByNameInOrg(name, organizationId);
        MDCBuilder.buildMdcContext(stack);
        if (!cloudParameterCache.isScalingSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Scaling is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        updateRequest.setStackId(stack.getId());
        UpdateStackJson updateStackJson = conversionService.convert(updateRequest, UpdateStackJson.class);
        if (updateStackJson.getInstanceGroupAdjustment().getScalingAdjustment() > 0) {
            return put(stack, updateStackJson);
        } else {
            UpdateClusterJson updateClusterJson = conversionService.convert(updateRequest, UpdateClusterJson.class);
            return clusterCommonService.put(stack.getId(), updateClusterJson);
        }
    }

    public void deleteWithKerbereosInOrg(String name, Long organizationId, Boolean withStackDelete, Boolean deleteDependencies) {
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(organizationId, OrganizationResource.STACK, Action.WRITE);
        Stack stack = stackService.getByNameInOrg(name, organizationId);
        MDCBuilder.buildMdcContext(stack);
        clusterService.delete(stack.getId(), withStackDelete, deleteDependencies);
    }

    public void repairCluster(Long organizationId, String name, ClusterRepairRequest clusterRepairRequest) {
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(organizationId, OrganizationResource.STACK, Action.WRITE);
        Stack stack = stackService.getByNameInOrg(name, organizationId);
        clusterService.repairCluster(stack.getId(), clusterRepairRequest.getHostGroups(), clusterRepairRequest.isRemoveOnly());
    }

    public void retryInOrganization(String name, Long organizationId) {
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(organizationId, OrganizationResource.STACK, Action.WRITE);
        Stack stack = stackService.getByNameInOrg(name, organizationId);
        operationRetryService.retry(stack);
    }

    private Response put(Stack stack, UpdateStackJson updateRequest) {
        MDCBuilder.buildMdcContext(stack);
        if (updateRequest.getStatus() != null) {
            stackService.updateStatus(stack.getId(), updateRequest.getStatus(), updateRequest.getWithClusterEvent());
        } else {
            Integer scalingAdjustment = updateRequest.getInstanceGroupAdjustment().getScalingAdjustment();
            validateHardLimits(scalingAdjustment);
            validateAccountPreferences(stack.getId(), scalingAdjustment);
            stackService.updateNodeCount(stack, updateRequest.getInstanceGroupAdjustment(), updateRequest.getWithClusterEvent());
        }
        return Response.status(Status.NO_CONTENT).build();
    }

    public GeneratedBlueprintResponse postStackForBlueprint(StackV2Request stackRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        stackRequest.setAccount(user.getAccount());
        stackRequest.setOwner(user.getUserId());
        BlueprintPreparationObject blueprintPreparationObject = conversionService.convert(stackRequest, BlueprintPreparationObject.class);
        String blueprintText = centralBlueprintUpdater.getBlueprintText(blueprintPreparationObject);
        return new GeneratedBlueprintResponse(blueprintText);
    }

    @Override
    public CertificateResponse getCertificate(Long stackId) {
        return tlsSecurityService.getCertificates(stackId);
    }

    @Override
    public StackResponse getStackForAmbari(AmbariAddressJson json) {
        return stackService.getByAmbariAddress(json.getAmbariAddress());
    }

    @Override
    public Set<AutoscaleStackResponse> getAllForAutoscale() {
        LOGGER.info("Get all stack, autoscale authorized only.");
        return stackService.getAllForAutoscale();
    }

    @Override
    public Response validate(StackValidationRequest request) {
        StackValidation stackValidation = conversionService.convert(request, StackValidation.class);
        stackService.validateStack(stackValidation, true);
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(stackValidation.getCredential());
        fileSystemValidator.validateFileSystem(request.getPlatform(), cloudCredential, request.getFileSystem());
        return Response.status(Status.NO_CONTENT).build();
    }

    @Override
    public Response deleteInstance(Long stackId, String instanceId) {
        return deleteInstance(stackId, instanceId, false);
    }

    @Override
    public Response deleteInstance(Long stackId, String instanceId, boolean forced) {
        Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(organization.getId(), OrganizationResource.STACK, Action.WRITE);
        stackService.removeInstance(stackId, organization.getId(), instanceId, forced);
        return Response.status(Status.NO_CONTENT).build();
    }

    public Response deleteInstanceByNameInOrg(String name, Long organizationId, String instanceId, boolean forced) {
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(organizationId, OrganizationResource.STACK, Action.WRITE);
        Stack stack = stackService.getByNameInOrg(name, organizationId);
        stackService.removeInstance(stack, organizationId, instanceId, forced);
        return Response.status(Status.NO_CONTENT).build();
    }

    @Override
    public Response deleteInstances(Long stackId, Set<String> instanceIds) {
        Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
        stackService.removeInstances(stackId, organization.getId(), instanceIds);
        return Response.status(Status.NO_CONTENT).build();
    }

    @Override
    public PlatformVariantsJson variants() {
        PlatformVariants pv = parameterService.getPlatformVariants();
        return conversionService.convert(pv, PlatformVariantsJson.class);
    }

    public Response changeImageByNameInOrg(String name, Long organziationId, StackImageChangeRequest stackImageChangeRequest) {
        Stack stack = stackService.getByNameInOrg(name, organziationId);
        if (StringUtils.isNotBlank(stackImageChangeRequest.getImageCatalogName())) {
            ImageCatalog imageCatalog = imageCatalogService.get(organziationId, stackImageChangeRequest.getImageCatalogName());
            stackService.updateImage(stack.getId(), organziationId, stackImageChangeRequest.getImageId(),
                    imageCatalog.getName(), imageCatalog.getImageCatalogUrl());
        } else {
            stackService.updateImage(stack.getId(), organziationId, stackImageChangeRequest.getImageId(), null, null);
        }
        return Response.status(Status.NO_CONTENT).build();
    }

    private void validateAccountPreferences(Long stackId, Integer scalingAdjustment) {
        try {
            accountPreferencesValidator.validate(stackId, scalingAdjustment);
        } catch (AccountPreferencesValidationException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    private void validateHardLimits(Integer scalingAdjustment) {
        if (scalingHardLimitsService.isViolatingMaxUpscaleStepInNodeCount(scalingAdjustment)) {
            throw new BadRequestException(String.format("Upscaling by more than %d nodes is not supported",
                    scalingHardLimitsService.getMaxUpscaleStepInNodeCount()));
        }
    }

    public Set<StackResponse> retrieveStacksByOrganizationId(Long organizationId) {
        return stackService.retrieveStacksByOrganizationId(organizationId);
    }

    public StackResponse findStackByNameAndOrganizationId(String name, Long organizationId) {
        return stackService.findStackByNameAndOrganizationId(name, organizationId);
    }
}
