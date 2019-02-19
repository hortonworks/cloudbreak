package com.sequenceiq.cloudbreak.service;

import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.AmbariAddressV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.CertificateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackValidationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedClusterDefinitionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.authorization.PermissionCheckingUtils;
import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.clusterdefinition.CentralClusterDefinitionUpdater;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.type.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.controller.StackCreatorService;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.stack.StackApiViewService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Service
public class StackCommonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCommonService.class);

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ScalingHardLimitsService scalingHardLimitsService;

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Inject
    private CentralClusterDefinitionUpdater centralClusterDefinitionUpdater;

    @Inject
    private OperationRetryService operationRetryService;

    @Inject
    private ClusterCommonService clusterCommonService;

    @Inject
    private CloudParameterCache cloudParameterCache;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private FileSystemValidator fileSystemValidator;

    @Inject
    private StackCreatorService stackCreatorService;

    @Inject
    private StackApiViewService stackApiViewService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private StackService stackService;

    @Inject
    private UserService userService;

    public StackV4Response createInWorkspace(StackV4Request stackRequest, CloudbreakUser cloudbreakUser, User user, Workspace workspace) {
        return stackCreatorService.createStack(cloudbreakUser, user, workspace, stackRequest);
    }

    public void deleteInWorkspace(String name, Long workspaceId, Boolean forced, Boolean deleteDependencies, User user) {
        stackService.delete(name, workspaceId, forced, deleteDependencies, user);
    }

    public StackV4Response get(Long id, Set<String> entries) {
        return stackService.getJsonById(id, entries);
    }

    public Set<StackViewV4Response> retrieveStacksByWorkspaceId(Long workspaceId, String environment, boolean onlyDatalakes) {
        return stackApiViewService.retrieveStackViewsByWorkspaceId(workspaceId, environment, onlyDatalakes);
    }

    public StackV4Response findStackByNameAndWorkspaceId(String name, Long workspaceId, Set<String> entries) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return stackService.getByNameInWorkspaceWithEntries(name, workspaceId, entries, user);
    }

    public void putInDefaultWorkspace(Long id, UpdateStackV4Request updateRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(restRequestThreadLocalService.getRequestedWorkspaceId(),
                WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getById(id);
        MDCBuilder.buildMdcContext(stack);
        put(stack, updateRequest);
    }

    public void putStopInWorkspace(String name, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        if (!cloudParameterCache.isStartStopSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Stop is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setStatus(StatusRequest.STOPPED);
        updateStackJson.setWithClusterEvent(true);
        put(stack, updateStackJson);
    }

    public void putSyncInWorkspace(String name, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setStatus(StatusRequest.FULL_SYNC);
        updateStackJson.setWithClusterEvent(true);
        put(stack, updateStackJson);
    }

    public void putStartInWorkspace(String name, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        if (!cloudParameterCache.isStartStopSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Start is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setStatus(StatusRequest.STARTED);
        updateStackJson.setWithClusterEvent(true);
        put(stack, updateStackJson);
    }

    public void putScalingInWorkspace(String name, Long workspaceId, StackScaleV4Request updateRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        updateRequest.setStackId(stack.getId());
        UpdateStackV4Request updateStackJson = converterUtil.convert(updateRequest, UpdateStackV4Request.class);
        Integer scalingAdjustment = updateStackJson.getInstanceGroupAdjustment().getScalingAdjustment();
        if (scalingAdjustment > 0 && !cloudParameterCache.isUpScalingSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Upscaling is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        if (scalingAdjustment < 0 && !cloudParameterCache.isDownScalingSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Downscaling is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        if (scalingAdjustment > 0) {
            put(stack, updateStackJson);
        } else {
            UpdateClusterV4Request updateClusterJson = converterUtil.convert(updateRequest, UpdateClusterV4Request.class);
            Workspace workspace = workspaceService.get(workspaceId, user);
            clusterCommonService.put(stack.getId(), updateClusterJson, user, workspace);
        }
    }

    public void deleteWithKerbereosInWorkspace(String name, Long workspaceId, Boolean withStackDelete, Boolean deleteDependencies) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        clusterService.delete(stack.getId(), withStackDelete, deleteDependencies);
    }

    public void repairCluster(Long workspaceId, String name, ClusterRepairV4Request clusterRepairRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        if (clusterRepairRequest.getHostGroups() != null) {
            clusterService.repairCluster(stack.getId(), clusterRepairRequest.getHostGroups(), clusterRepairRequest.isRemoveOnly());
        } else {
            clusterService.repairCluster(stack.getId(), clusterRepairRequest.getNodes().getIds(),
                    clusterRepairRequest.getNodes().isDeleteVolumes(), clusterRepairRequest.isRemoveOnly());
        }
    }

    public void retryInWorkspace(String name, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        operationRetryService.retry(stack);
    }

    public GeneratedClusterDefinitionV4Response postStackForClusterDefinition(StackV4Request stackRequest) {
        TemplatePreparationObject templatePreparationObject = converterUtil.convert(stackRequest, TemplatePreparationObject.class);
        String clusterDefinitionText = centralClusterDefinitionUpdater.getClusterDefinitionText(templatePreparationObject);
        GeneratedClusterDefinitionV4Response response = new GeneratedClusterDefinitionV4Response();
        response.setClusterDefinitionText(clusterDefinitionText);
        return response;
    }

    @PreAuthorize("#oauth2.hasScope('cloudbreak.autoscale')")
    public CertificateV4Response getCertificate(Long stackId) {
        return tlsSecurityService.getCertificates(stackId);
    }

    public StackV4Response getStackForAmbari(AmbariAddressV4Request json) {
        return stackService.getByAmbariAddress(json.getAmbariAddress());
    }

    public Set<AutoscaleStackV4Response> getAllForAutoscale() {
        LOGGER.debug("Get all stack, autoscale authorized only.");
        return stackService.getAllForAutoscale();
    }

    public void validate(StackValidationV4Request request) {
        StackValidation stackValidation = converterUtil.convert(request, StackValidation.class);
        stackService.validateStack(stackValidation);
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(stackValidation.getCredential());
        fileSystemValidator.validateFileSystem(stackValidation.getCredential().cloudPlatform(), cloudCredential, request.getFileSystem(), null, null);
    }

    public void deleteInstanceByNameInWorkspace(String name, Long workspaceId, String instanceId, boolean forced) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        stackService.removeInstance(stack, workspaceId, instanceId, forced, user);
    }

    public void changeImageByNameInWorkspace(String name, Long organziationId, StackImageChangeV4Request stackImageChangeRequest) {
        Stack stack = stackService.getByNameInWorkspace(name, organziationId);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        if (StringUtils.isNotBlank(stackImageChangeRequest.getImageCatalogName())) {
            ImageCatalog imageCatalog = imageCatalogService.get(organziationId, stackImageChangeRequest.getImageCatalogName());
            stackService.updateImage(stack.getId(), organziationId, stackImageChangeRequest.getImageId(),
                    imageCatalog.getName(), imageCatalog.getImageCatalogUrl(), user);
        } else {
            stackService.updateImage(stack.getId(), organziationId, stackImageChangeRequest.getImageId(), null, null, user);
        }
    }

    private void put(Stack stack, UpdateStackV4Request updateRequest) {
        MDCBuilder.buildMdcContext(stack);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        if (updateRequest.getStatus() != null) {
            stackService.updateStatus(stack.getId(), updateRequest.getStatus(), updateRequest.getWithClusterEvent(), user);
        } else {
            Integer scalingAdjustment = updateRequest.getInstanceGroupAdjustment().getScalingAdjustment();
            validateHardLimits(scalingAdjustment);
            stackService.updateNodeCount(stack, updateRequest.getInstanceGroupAdjustment(), updateRequest.getWithClusterEvent(), user);
        }
    }

    private void validateHardLimits(Integer scalingAdjustment) {
        if (scalingHardLimitsService.isViolatingMaxUpscaleStepInNodeCount(scalingAdjustment)) {
            throw new BadRequestException(String.format("Upscaling by more than %d nodes is not supported",
                    scalingHardLimitsService.getMaxUpscaleStepInNodeCount()));
        }
    }

}
