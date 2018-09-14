package com.sequenceiq.cloudbreak.service;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.QueryParam;
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
import com.sequenceiq.cloudbreak.authorization.PermissionCheckingUtils;
import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
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
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidationException;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidator;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.decorator.StackDecorator;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

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
    private WorkspaceService workspaceService;

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

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    public StackResponse createInWorkspace(StackRequest stackRequest, IdentityUser identityUser, User user, Workspace workspace) {
        return stackCreatorService.createStack(identityUser, user, workspace, stackRequest);
    }

    public void deleteInWorkspace(String name, Long workspaceId, Boolean forced, Boolean deleteDependencies, User user) {
        stackService.delete(name, workspaceId, forced, deleteDependencies, user);
    }

    @Override
    public Set<StackResponse> getStacksInDefaultWorkspace() {
        return stackService.retrieveStacksByWorkspaceId(restRequestThreadLocalService.getRequestedWorkspaceId());
    }

    @Override
    public StackResponse get(Long id, Set<String> entries) {
        return stackService.getJsonById(id, entries);
    }

    @Override
    public StackResponse getStackFromDefaultWorkspace(String name, Set<String> entries) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return stackService.getStackByNameInWorkspace(name, entries, workspace);
    }

    @Override
    public Map<String, Object> status(Long id) {
        return conversionService.convert(stackService.getById(id), Map.class);
    }

    @Override
    public void deleteById(Long id, Boolean forced, Boolean deleteDependencies) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        stackService.delete(id, forced, deleteDependencies, user);
    }

    @Override
    public void deleteInDefaultWorkspace(String name, Boolean forced, Boolean deleteDependencies) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        stackService.delete(name, restRequestThreadLocalService.getRequestedWorkspaceId(), forced, deleteDependencies, user);
    }

    public Set<StackResponse> retrieveStacksByWorkspaceId(Long workspaceId) {
        return stackService.retrieveStacksByWorkspaceId(workspaceId);
    }

    public StackResponse findStackByNameAndWorkspaceId(String name, Long workspaceId, @QueryParam("entry") Set<String> entries) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        return stackService.getByNameInWorkspaceWithEntries(name, workspaceId, entries, user);
    }

    public Response putInDefaultWorkspace(Long id, UpdateStackJson updateRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(restRequestThreadLocalService.getRequestedWorkspaceId(),
                WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getById(id);
        MDCBuilder.buildMdcContext(stack);
        return put(stack, updateRequest);
    }

    public Response putStopInWorkspace(String name, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        if (!cloudParameterCache.isStartStopSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Stop is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setStatus(StatusRequest.STOPPED);
        updateStackJson.setWithClusterEvent(true);
        return put(stack, updateStackJson);
    }

    public Response putSyncInWorkspace(String name, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setStatus(StatusRequest.FULL_SYNC);
        updateStackJson.setWithClusterEvent(true);
        return put(stack, updateStackJson);
    }

    public Response putStartInWorkspace(String name, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        if (!cloudParameterCache.isStartStopSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Start is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setStatus(StatusRequest.STARTED);
        updateStackJson.setWithClusterEvent(true);
        return put(stack, updateStackJson);
    }

    public Response putScalingInWorkspace(String name, Long workspaceId, StackScaleRequestV2 updateRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        updateRequest.setStackId(stack.getId());
        UpdateStackJson updateStackJson = conversionService.convert(updateRequest, UpdateStackJson.class);
        Integer scalingAdjustment = updateStackJson.getInstanceGroupAdjustment().getScalingAdjustment();
        if (scalingAdjustment > 0 && !cloudParameterCache.isUpScalingSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Upscaling is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        if (scalingAdjustment < 0 && !cloudParameterCache.isDownScalingSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Downscaling is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        if (scalingAdjustment > 0) {
            return put(stack, updateStackJson);
        } else {
            UpdateClusterJson updateClusterJson = conversionService.convert(updateRequest, UpdateClusterJson.class);
            Workspace workspace = workspaceService.get(workspaceId, user);
            return clusterCommonService.put(stack.getId(), updateClusterJson, user, workspace);
        }
    }

    public void deleteWithKerbereosInWorkspace(String name, Long workspaceId, Boolean withStackDelete, Boolean deleteDependencies) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        clusterService.delete(stack.getId(), withStackDelete, deleteDependencies);
    }

    public void repairCluster(Long workspaceId, String name, ClusterRepairRequest clusterRepairRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        clusterService.repairCluster(stack.getId(), clusterRepairRequest.getHostGroups(), clusterRepairRequest.isRemoveOnly());
    }

    public void retryInWorkspace(String name, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        operationRetryService.retry(stack);
    }

    private Response put(Stack stack, UpdateStackJson updateRequest) {
        MDCBuilder.buildMdcContext(stack);
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        if (updateRequest.getStatus() != null) {
            stackService.updateStatus(stack.getId(), updateRequest.getStatus(), updateRequest.getWithClusterEvent(), user);
        } else {
            Integer scalingAdjustment = updateRequest.getInstanceGroupAdjustment().getScalingAdjustment();
            validateHardLimits(scalingAdjustment);
            validateAccountPreferences(stack.getId(), scalingAdjustment);
            stackService.updateNodeCount(stack, updateRequest.getInstanceGroupAdjustment(), updateRequest.getWithClusterEvent(), user);
        }
        return Response.status(Status.NO_CONTENT).build();
    }

    public GeneratedBlueprintResponse postStackForBlueprint(StackV2Request stackRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        stackRequest.setOwner(user.getUserId());
        TemplatePreparationObject templatePreparationObject = conversionService.convert(stackRequest, TemplatePreparationObject.class);
        String blueprintText = centralBlueprintUpdater.getBlueprintText(templatePreparationObject);
        return new GeneratedBlueprintResponse(blueprintText);
    }

    public CertificateResponse getCertificate(Long stackId) {
        return tlsSecurityService.getCertificates(stackId);
    }

    public StackResponse getStackForAmbari(AmbariAddressJson json) {
        return stackService.getByAmbariAddress(json.getAmbariAddress());
    }

    public Set<AutoscaleStackResponse> getAllForAutoscale() {
        LOGGER.info("Get all stack, autoscale authorized only.");
        return stackService.getAllForAutoscale();
    }

    @Override
    public Response validate(StackValidationRequest request) {
        StackValidation stackValidation = conversionService.convert(request, StackValidation.class);
        stackService.validateStack(stackValidation, true);
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(stackValidation.getCredential());
        fileSystemValidator.validateFileSystem(request.getPlatform(), cloudCredential, request.getFileSystem(), null, null);
        return Response.status(Status.NO_CONTENT).build();
    }

    @Override
    public Response deleteInstance(Long stackId, String instanceId) {
        return deleteInstance(stackId, instanceId, false);
    }

    @Override
    public Response deleteInstance(Long stackId, String instanceId, boolean forced) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(restRequestThreadLocalService.getRequestedWorkspaceId(), WorkspaceResource.STACK,
                Action.WRITE, user);
        stackService.removeInstance(stackId, restRequestThreadLocalService.getRequestedWorkspaceId(), instanceId, forced, user);
        return Response.status(Status.NO_CONTENT).build();
    }

    public Response deleteInstanceByNameInWorkspace(String name, Long workspaceId, String instanceId, boolean forced) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        stackService.removeInstance(stack, workspaceId, instanceId, forced, user);
        return Response.status(Status.NO_CONTENT).build();
    }

    @Override
    public Response deleteInstances(Long stackId, Set<String> instanceIds) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        stackService.removeInstances(stackId, restRequestThreadLocalService.getRequestedWorkspaceId(), instanceIds, user);
        return Response.status(Status.NO_CONTENT).build();
    }

    @Override
    public PlatformVariantsJson variants() {
        PlatformVariants pv = parameterService.getPlatformVariants();
        return conversionService.convert(pv, PlatformVariantsJson.class);
    }

    public Response changeImageByNameInWorkspace(String name, Long organziationId, StackImageChangeRequest stackImageChangeRequest) {
        Stack stack = stackService.getByNameInWorkspace(name, organziationId);
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        if (StringUtils.isNotBlank(stackImageChangeRequest.getImageCatalogName())) {
            ImageCatalog imageCatalog = imageCatalogService.get(organziationId, stackImageChangeRequest.getImageCatalogName());
            stackService.updateImage(stack.getId(), organziationId, stackImageChangeRequest.getImageId(),
                    imageCatalog.getName(), imageCatalog.getImageCatalogUrl(), user);
        } else {
            stackService.updateImage(stack.getId(), organziationId, stackImageChangeRequest.getImageId(), null, null, user);
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
}
