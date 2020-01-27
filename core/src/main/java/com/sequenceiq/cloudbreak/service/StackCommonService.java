package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.AmbariAddressV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.CertificateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn.NameOrCrnReader;
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
import com.sequenceiq.cloudbreak.retry.RetryableFlow;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.template.BlueprintUpdaterConnectors;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.workspace.authorization.PermissionCheckingUtils;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

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
    private ClusterRepairService clusterRepairService;

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

    public StackV4Response get(Long id, Set<String> entries) {
        return stackService.getJsonById(id, entries);
    }

    public StackV4Response getByCrn(String crn, Set<String> entries) {
        return stackService.getJsonByCrn(crn, entries);
    }

    public StackV4Response findStackByNameOrCrnAndWorkspaceId(NameOrCrn nameOrCrn, Long workspaceId, Set<String> entries, StackType stackType) {
        NameOrCrnReader reader = NameOrCrnReader.create(nameOrCrn);
        return reader.hasName()
                ? findStackByNameAndWorkspaceId(reader.getName(), workspaceId, entries, stackType)
                : findStackByCrnAndWorkspaceId(reader.getCrn(), workspaceId, entries, stackType);
    }

    private StackV4Response findStackByNameAndWorkspaceId(String name, Long workspaceId, Set<String> entries, StackType stackType) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return stackService.getByNameInWorkspaceWithEntries(name, workspaceId, entries, user, stackType);
    }

    private StackV4Response findStackByCrnAndWorkspaceId(String crn, Long workspaceId, Set<String> entries, StackType stackType) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return stackService.getByCrnInWorkspaceWithEntries(crn, workspaceId, entries, user, stackType);
    }

    public void putInDefaultWorkspace(String crn, UpdateStackV4Request updateRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        Stack stack = stackService.getByCrn(crn);
        MDCBuilder.buildMdcContext(stack);
        put(stack, updateRequest);
    }

    public void putStopInWorkspace(NameOrCrn nameOrCrn, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        if (!cloudParameterCache.isStartStopSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Stop is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setStatus(StatusRequest.STOPPED);
        updateStackJson.setWithClusterEvent(true);
        put(stack, updateStackJson);
    }

    public void syncInWorkspace(NameOrCrn nameOrCrn, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setStatus(StatusRequest.FULL_SYNC);
        updateStackJson.setWithClusterEvent(true);
        put(stack, updateStackJson);
    }

    public void putStartInWorkspace(NameOrCrn nameOrCrn, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        putStartInWorkspace(stack);
    }

    private void putStartInWorkspace(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        if (!cloudParameterCache.isStartStopSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Start is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setStatus(StatusRequest.STARTED);
        updateStackJson.setWithClusterEvent(true);
        put(stack, updateStackJson);
    }

    public void putScalingInWorkspace(NameOrCrn nameOrCrn, Long workspaceId, StackScaleV4Request updateRequest) {
        User user = checkUserPermission();
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
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

    public void deleteWithKerberosInWorkspace(NameOrCrn nameOrCrn, Long workspaceId, boolean forced) {
        checkUserPermission();
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        clusterService.delete(stack.getId(), forced);
    }

    public void repairCluster(Long workspaceId, NameOrCrn nameOrCrn, ClusterRepairV4Request clusterRepairRequest) {
        checkUserPermission();
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        if (clusterRepairRequest.getHostGroups() != null) {
            clusterRepairService.repairHostGroups(stack.getId(), new HashSet<>(clusterRepairRequest.getHostGroups()), clusterRepairRequest.isRemoveOnly());
        } else {
            clusterRepairService.repairNodes(stack.getId(),
                    new HashSet<>(clusterRepairRequest.getNodes().getIds()),
                    clusterRepairRequest.getNodes().isDeleteVolumes(),
                    clusterRepairRequest.isRemoveOnly());
        }
    }

    public void retryInWorkspace(NameOrCrn nameOrCrn, Long workspaceId) {
        checkUserPermission();
        Long stackId = stackService.getIdByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        operationRetryService.retry(stackId);
    }

    private User checkUserPermission() {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        return user;
    }

    public List<RetryableFlow> getRetryableFlows(String name, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        Long stackId = stackService.getIdByNameInWorkspace(name, workspaceId);
        return operationRetryService.getRetryableFlows(stackId);
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

    public void deleteInstanceInWorkspace(NameOrCrn nameOrCrn, Long workspaceId, String instanceId, boolean forced) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        stackService.removeInstance(stack, workspaceId, instanceId, forced, user);
    }

    public void deleteMultipleInstancesInWorkspace(NameOrCrn nameOrCrn, Long workspaceId, List<String> instanceIds, boolean forced) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        stackService.removeInstances(stack, workspaceId, instanceIds, forced, user);
    }

    public void changeImageInWorkspace(NameOrCrn nameOrCrn, Long organziationId, StackImageChangeV4Request stackImageChangeRequest) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, organziationId);
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
