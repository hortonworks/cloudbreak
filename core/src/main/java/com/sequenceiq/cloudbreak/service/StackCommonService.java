package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;

import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.AmbariAddressV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.CertificateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackValidationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.authorization.PermissionCheckingUtils;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.controller.StackCreatorService;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.template.BlueprintUpdaterConnectors;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.resource.ResourceAction;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;

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

    @Inject
    private BlueprintUpdaterConnectors blueprintUpdaterConnectors;

    public StackV4Response createInWorkspace(StackV4Request stackRequest, User user, Workspace workspace) {
        return stackCreatorService.createStack(user, workspace, stackRequest);
    }

    public void deleteByNameInWorkspace(String name, Long workspaceId, Boolean forced, Boolean deleteDependencies, User user) {
        stackService.deleteByName(name, workspaceId, forced, deleteDependencies, user);
    }

    public void deleteByCrnInWorkspace(String crn, Long workspaceId, Boolean forced, Boolean deleteDependencies, User user) {
        stackService.deleteByCrn(crn, workspaceId, forced, deleteDependencies, user);
    }

    public StackV4Response get(Long id, Set<String> entries) {
        return stackService.getJsonById(id, entries);
    }

    public StackV4Response getByCrn(String crn, Set<String> entries) {
        return stackService.getJsonByCrn(crn, entries);
    }

    public StackV4Response findStackByNameAndWorkspaceId(String name, Long workspaceId, Set<String> entries, StackType stackType) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return stackService.getByNameInWorkspaceWithEntries(name, workspaceId, entries, user, stackType);
    }

    public StackV4Response findStackByCrnAndWorkspaceId(String crn, Long workspaceId, Set<String> entries, StackType stackType) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return stackService.getByCrnInWorkspaceWithEntries(crn, workspaceId, entries, user, stackType);
    }

    public void putInDefaultWorkspace(String crn, UpdateStackV4Request updateRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(restRequestThreadLocalService.getRequestedWorkspaceId(),
                WorkspaceResource.STACK, ResourceAction.WRITE, user);
        Stack stack = stackService.getByCrn(crn);
        MDCBuilder.buildMdcContext(stack);
        put(stack, updateRequest);
    }

    public void putStopInWorkspaceByName(String name, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, ResourceAction.WRITE, user);
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

    public void putStopInWorkspaceByCrn(String crn, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, ResourceAction.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(crn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        if (!cloudParameterCache.isStartStopSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Stop is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setStatus(StatusRequest.STOPPED);
        updateStackJson.setWithClusterEvent(true);
        put(stack, updateStackJson);
    }

    public void putSyncInWorkspace(String name, String crn, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, ResourceAction.WRITE, user);
        Stack stack;
        if (!Strings.isNullOrEmpty(name)) {
            stack = stackService.getByNameInWorkspace(name, workspaceId);
        } else {
            stack = stackService.getByCrnInWorkspace(crn, workspaceId);
        }
        MDCBuilder.buildMdcContext(stack);
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setStatus(StatusRequest.FULL_SYNC);
        updateStackJson.setWithClusterEvent(true);
        put(stack, updateStackJson);
    }

    public void putStartInWorkspace(String name, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, ResourceAction.WRITE, user);
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
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, ResourceAction.WRITE, user);
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
            clusterCommonService.put(stack.getResourceCrn(), updateClusterJson, user, workspace);
        }
    }

    public void deleteWithKerberosByNameInWorkspace(String name, Long workspaceId, Boolean withStackDelete, Boolean deleteDependencies) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, ResourceAction.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        clusterService.delete(stack.getId(), withStackDelete, deleteDependencies);
    }

    public void deleteWithKerberosByCrnInWorkspace(String crn, Long workspaceId, Boolean withStackDelete, Boolean deleteDependencies) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, ResourceAction.WRITE, user);
        Stack stack = stackService.getByCrnInWorkspace(crn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        clusterService.delete(stack.getId(), withStackDelete, deleteDependencies);
    }

    public void repairClusterByName(Long workspaceId, String name, ClusterRepairV4Request clusterRepairRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, ResourceAction.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        if (clusterRepairRequest.getHostGroups() != null) {
            clusterService.repairCluster(stack.getId(), clusterRepairRequest.getHostGroups(), clusterRepairRequest.isRemoveOnly());
        } else {
            clusterService.repairCluster(stack.getId(), clusterRepairRequest.getNodes().getIds(),
                    clusterRepairRequest.getNodes().isDeleteVolumes(), clusterRepairRequest.isRemoveOnly());
        }
    }

    public void repairClusterByCrn(Long workspaceId, String crn, ClusterRepairV4Request clusterRepairRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, ResourceAction.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(crn, workspaceId);
        if (clusterRepairRequest.getHostGroups() != null) {
            clusterService.repairCluster(stack.getId(), clusterRepairRequest.getHostGroups(), clusterRepairRequest.isRemoveOnly());
        } else {
            clusterService.repairCluster(stack.getId(), clusterRepairRequest.getNodes().getIds(),
                    clusterRepairRequest.getNodes().isDeleteVolumes(), clusterRepairRequest.isRemoveOnly());
        }
    }

    public void retryInWorkspaceByName(String name, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, ResourceAction.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        operationRetryService.retry(stack);
    }

    public void retryInWorkspaceByCrn(String crn, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, ResourceAction.WRITE, user);
        Stack stack = stackService.getByCrnInWorkspace(crn, workspaceId);
        operationRetryService.retry(stack);
    }

    public GeneratedBlueprintV4Response postStackForBlueprint(StackV4Request stackRequest) {
        TemplatePreparationObject templatePreparationObject = converterUtil.convert(stackRequest, TemplatePreparationObject.class);

        String blueprintText = blueprintUpdaterConnectors.getBlueprintText(templatePreparationObject);
        GeneratedBlueprintV4Response response = new GeneratedBlueprintV4Response();
        response.setBlueprintText(anonymize(blueprintText));
        return response;
    }

    @PreAuthorize("hasRole('AUTOSCALE')")
    public CertificateV4Response getCertificate(String crn) {
        Stack stack = stackService.findByCrn(crn);
        return tlsSecurityService.getCertificates(stack.getId());
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
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, ResourceAction.WRITE, user);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        stackService.removeInstance(stack, workspaceId, instanceId, forced, user);
    }

    public void deleteInstanceByCrnInWorkspace(String crn, Long workspaceId, String instanceId, boolean forced) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, ResourceAction.WRITE, user);
        Stack stack = stackService.getByCrnInWorkspace(crn, workspaceId);
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

    public void changeImageByCrnInWorkspace(String crn, Long organziationId, StackImageChangeV4Request stackImageChangeRequest) {
        Stack stack = stackService.getByCrnInWorkspace(crn, organziationId);
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
