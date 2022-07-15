package com.sequenceiq.distrox.v1.distrox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.HierarchyAuthResourcePropertyProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.MaintenanceModeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.AttachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.DetachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.UpdateRecipesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CertificatesRotationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.AttachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.DetachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.UpdateRecipesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryValidationV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackClusterStatusViewToStatusConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.UserNamePasswordV4RequestToUpdateClusterV4RequestConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.view.StackApiViewToStackViewV4ResponseConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.detach.StackUpdateService;
import com.sequenceiq.cloudbreak.domain.projection.StackClusterStatusView;
import com.sequenceiq.cloudbreak.domain.projection.StackCrnView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;
import com.sequenceiq.cloudbreak.service.DatabaseBackupRestoreService;
import com.sequenceiq.cloudbreak.service.LoadBalancerUpdateService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.image.GenerateImageCatalogService;
import com.sequenceiq.cloudbreak.service.publicendpoint.GatewayPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.StackApiViewService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterRecoveryService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.distrox.v1.distrox.service.EnvironmentServiceDecorator;
import com.sequenceiq.distrox.v1.distrox.service.SdxServiceDecorator;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.RetryableFlow;

@Service
public class StackOperations implements HierarchyAuthResourcePropertyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackOperations.class);

    @Inject
    private StackCommonService stackCommonService;

    @Inject
    private StackUpdateService stackUpdateService;

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private StackService stackService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterCommonService clusterCommonService;

    @Inject
    private StackApiViewService stackApiViewService;

    @Inject
    private EnvironmentServiceDecorator environmentServiceDecorator;

    @Inject
    private SdxServiceDecorator sdxServiceDecorator;

    @Inject
    private ClusterRecoveryService recoveryService;

    @Inject
    private DatabaseBackupRestoreService databaseBackupRestoreService;

    @Inject
    private LoadBalancerUpdateService loadBalancerUpdateService;

    @Inject
    private StackApiViewToStackViewV4ResponseConverter stackApiViewToStackViewV4ResponseConverter;

    @Inject
    private StackClusterStatusViewToStatusConverter stackClusterStatusViewToStatusConverter;

    @Inject
    private UserNamePasswordV4RequestToUpdateClusterV4RequestConverter userNamePasswordV4RequestToUpdateClusterV4RequestConverter;

    @Inject
    private StackImageService stackImageService;

    @Inject
    private GenerateImageCatalogService generateImageCatalogService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private ClusterOperationService clusterOperationService;

    @Inject
    private GatewayPublicEndpointManagementService gatewayPublicEndpointManagementService;

    public StackViewV4Responses listByEnvironmentName(Long workspaceId, String environmentName, List<StackType> stackTypes) {
        Set<StackViewV4Response> stackViewResponses;
        LOGGER.info("List for Stack in workspace {} and environmentName {}.", workspaceId, environmentName);
        stackViewResponses = stackApiViewService.retrieveStackViewsByWorkspaceIdAndEnvironmentName(workspaceId, environmentName, stackTypes)
                .stream()
                .map(s -> stackApiViewToStackViewV4ResponseConverter.convert(s))
                .collect(Collectors.toSet());
        LOGGER.info("Adding environment name and credential to the responses.");
        NameOrCrn nameOrCrn = StringUtils.isEmpty(environmentName) ? NameOrCrn.empty() : NameOrCrn.ofName(environmentName);
        environmentServiceDecorator.prepareEnvironmentsAndCredentialName(stackViewResponses, nameOrCrn);
        LOGGER.info("Adding SDX CRN and name to the responses.");
        sdxServiceDecorator.prepareMultipleSdxAttributes(stackViewResponses);
        return new StackViewV4Responses(stackViewResponses);
    }

    public StackViewV4Responses listByEnvironmentCrn(Long workspaceId, String environmentCrn, List<StackType> stackTypes) {
        Set<StackViewV4Response> stackViewResponses;
        LOGGER.info("List for Stack in workspace {} and environmentCrn {}.", workspaceId, environmentCrn);
        stackViewResponses = stackApiViewService.retrieveStackViewsByWorkspaceIdAndEnvironmentCrn(workspaceId, environmentCrn, stackTypes)
                .stream()
                .map(s -> stackApiViewToStackViewV4ResponseConverter.convert(s))
                .collect(Collectors.toSet());
        LOGGER.info("Adding environment name and credential to the responses.");
        NameOrCrn nameOrCrn = StringUtils.isEmpty(environmentCrn) ? NameOrCrn.empty() : NameOrCrn.ofCrn(environmentCrn);
        environmentServiceDecorator.prepareEnvironmentsAndCredentialName(stackViewResponses, nameOrCrn);
        LOGGER.info("Adding SDX CRN and name to the responses.");
        sdxServiceDecorator.prepareMultipleSdxAttributes(stackViewResponses);
        return new StackViewV4Responses(stackViewResponses);
    }

    public StackViewV4Responses listByStackIds(Long workspaceId, List<Long> stackIds, String environmentCrn, List<StackType> stackTypes) {
        Set<StackViewV4Response> stackViewResponses;
        stackViewResponses = stackApiViewService.retrieveStackViewsByStackIdsAndEnvironmentCrn(workspaceId, stackIds, environmentCrn, stackTypes)
                .stream()
                .map(s -> stackApiViewToStackViewV4ResponseConverter.convert(s))
                .collect(Collectors.toSet());
        LOGGER.info("Adding environment name and credential to the responses.");
        NameOrCrn nameOrCrn = Strings.isNullOrEmpty(environmentCrn) ? NameOrCrn.empty() : NameOrCrn.ofCrn(environmentCrn);
        environmentServiceDecorator.prepareEnvironmentsAndCredentialName(stackViewResponses, nameOrCrn);
        LOGGER.info("Adding SDX CRN and name to the responses.");
        sdxServiceDecorator.prepareMultipleSdxAttributes(stackViewResponses);
        return new StackViewV4Responses(stackViewResponses);
    }

    public StackV4Response post(Long workspaceId, CloudbreakUser cloudbreakUser, @Valid StackV4Request request, boolean distroxRequest) {
        LOGGER.info("Post for Stack in workspace {}.", workspaceId);
        User user = userService.getOrCreate(cloudbreakUser);
        LOGGER.info("Cloudbreak user for the requested stack is {}.", cloudbreakUser);
        Workspace workspace = workspaceService.get(workspaceId, user);
        StackV4Response stackV4Response = stackCommonService.createInWorkspace(request, user, workspace, distroxRequest);
        LOGGER.info("Adding environment name and credential to the response.");
        environmentServiceDecorator.prepareEnvironmentAndCredentialName(stackV4Response);
        LOGGER.info("Adding SDX CRN and name to the response.");
        sdxServiceDecorator.prepareSdxAttributes(stackV4Response);
        return stackV4Response;
    }

    public StackV4Response get(NameOrCrn nameOrCrn, String accountId, Set<String> entries, StackType stackType) {
        LOGGER.info("Validate stack in account {}.", accountId);
        StackV4Response stackResponse = stackCommonService.findStackByNameOrCrnAndWorkspaceId(nameOrCrn, accountId, entries, stackType);
        LOGGER.info("Adding environment name and credential to the response.");
        environmentServiceDecorator.prepareEnvironmentAndCredentialName(stackResponse);
        LOGGER.info("Adding SDX CRN and name to the response.");
        sdxServiceDecorator.prepareSdxAttributes(stackResponse);
        LOGGER.info("Query Stack successfully decorated.");
        return stackResponse;
    }

    public StackViewV4Response getForInternalCrn(NameOrCrn nameOrCrn, StackType stackType) {
        LOGGER.info("Validate stack against internal user.");
        StackApiView stackApiView = stackApiViewService.retrieveStackByCrnAndType(nameOrCrn.getCrn(), stackType);
        LOGGER.info("Query Stack (view) successfully finished with crn {}", nameOrCrn.getCrn());
        StackViewV4Response stackViewV4Response = stackApiViewToStackViewV4ResponseConverter.convert(stackApiView);
        LOGGER.info("Adding environment name to the response.");
        environmentServiceDecorator.prepareEnvironment(stackViewV4Response);
        return stackViewV4Response;
    }

    public StackStatusV4Responses getStatusForInternalCrns(List<String> crns, StackType stackType) {
        LOGGER.info("Get stackstatuses against internal user.");
        List<StackClusterStatusView> statuses = stackService.getStatusesByCrnsInternal(crns, stackType);
        LOGGER.info("Query Stack (status) successfully finished with crns {}", crns);
        return stackClusterStatusViewToStatusConverter.convert(statuses);
    }

    public FlowIdentifier deleteInstance(@NotNull NameOrCrn nameOrCrn, String accountId, boolean forced, String instanceId) {
        return stackCommonService.deleteInstanceInWorkspace(nameOrCrn, accountId, instanceId, forced);
    }

    public FlowIdentifier deleteInstances(NameOrCrn nameOrCrn, String accountId, Set<String> instanceIds, boolean forced) {
        return stackCommonService.deleteMultipleInstancesInWorkspace(nameOrCrn, accountId, instanceIds, forced);
    }

    public FlowIdentifier sync(NameOrCrn nameOrCrn, String accountId) {
        return stackCommonService.syncInWorkspace(nameOrCrn, accountId);
    }

    public FlowIdentifier syncComponentVersionsFromCm(NameOrCrn nameOrCrn, String accountId, Set<String> candidateImageUuids) {
        return stackCommonService.syncComponentVersionsFromCmInWorkspace(nameOrCrn, accountId, candidateImageUuids);
    }

    public FlowIdentifier retry(NameOrCrn nameOrCrn, Long workspaceId) {
        return stackCommonService.retryInWorkspace(nameOrCrn, workspaceId);
    }

    public FlowIdentifier putStop(NameOrCrn nameOrCrn, String accountId) {
        return stackCommonService.putStopInWorkspace(nameOrCrn, accountId);
    }

    public FlowIdentifier putStart(NameOrCrn nameOrCrn, String accountId) {
        return stackCommonService.putStartInWorkspace(nameOrCrn, accountId);
    }

    public FlowIdentifier rotateSaltPassword(NameOrCrn nameOrCrn, String accountId) {
        return stackCommonService.rotateSaltPassword(nameOrCrn, accountId);
    }

    public FlowIdentifier putScaling(@NotNull NameOrCrn nameOrCrn, String accountId, @Valid StackScaleV4Request updateRequest) {
        return stackCommonService.putScalingInWorkspace(nameOrCrn, accountId, updateRequest);
    }

    public FlowIdentifier repairCluster(@NotNull NameOrCrn nameOrCrn, Long workspaceId, @Valid ClusterRepairV4Request clusterRepairRequest) {
        LOGGER.debug("Starting to repair cluster with request: {}", clusterRepairRequest.toString());
        return stackCommonService.repairCluster(workspaceId, nameOrCrn, clusterRepairRequest);
    }

    public FlowIdentifier recoverCluster(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        LOGGER.debug("Starting to recover cluster ({}) from failed upgrade", nameOrCrn);
        return recoveryService.recoverCluster(workspaceId, nameOrCrn);
    }

    public FlowIdentifier updateSalt(@NotNull NameOrCrn nameOrCrn, String accountId) {
        LOGGER.debug("Starting salt update: " + nameOrCrn);
        return clusterCommonService.updateSalt(nameOrCrn, accountId);
    }

    public FlowIdentifier updatePillarConfiguration(@NotNull NameOrCrn nameOrCrn, String accountId) {
        LOGGER.debug("Starting pillar configuration update: " + nameOrCrn);
        return clusterCommonService.updatePillarConfiguration(nameOrCrn, accountId);
    }

    public GeneratedBlueprintV4Response postStackForBlueprint(@Valid StackV4Request stackRequest) {
        return stackCommonService.postStackForBlueprint(stackRequest);
    }

    public FlowIdentifier changeImage(@NotNull NameOrCrn nameOrCrn, Long workspaceId, @Valid StackImageChangeV4Request stackImageChangeRequest) {
        return stackCommonService.changeImageInWorkspace(nameOrCrn, workspaceId, stackImageChangeRequest);
    }

    public void delete(@NotNull NameOrCrn nameOrCrn, String accountId, boolean forced) {
        stackCommonService.deleteWithKerberosInWorkspace(nameOrCrn, accountId, forced);
    }

    public void updateNameAndCrn(@NotNull NameOrCrn nameOrCrn, Long workspaceId, String newName, String newCrn, boolean retainOriginalName) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        stackUpdateService.updateNameAndCrn(stack, newName, newCrn, retainOriginalName);
    }

    public StackV4Request getRequest(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        return stackService.getStackRequestByNameOrCrnInWorkspaceId(nameOrCrn, workspaceId);
    }

    public StackStatusV4Response getStatus(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        StackClusterStatusView stackStatusView = stackService.getStatusByNameOrCrn(nameOrCrn, workspaceId);
        return stackClusterStatusViewToStatusConverter.convert(stackStatusView);
    }

    public StackStatusV4Response getStatusByCrn(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        StackClusterStatusView stackStatusView = stackService.getStatusByNameOrCrn(nameOrCrn, workspaceId);
        return stackClusterStatusViewToStatusConverter.convert(stackStatusView);
    }

    public StackStatusV4Response getStatus(@NotNull String crn) {
        StackClusterStatusView stackStatusView = stackService.getStatusByCrn(crn);
        return stackClusterStatusViewToStatusConverter.convert(stackStatusView);
    }

    public FlowIdentifier putPassword(@NotNull NameOrCrn nameOrCrn, String accountId, @Valid UserNamePasswordV4Request userNamePasswordJson) {
        StackDto stack = stackDtoService.getByNameOrCrn(nameOrCrn, accountId);
        UpdateClusterV4Request updateClusterJson = userNamePasswordV4RequestToUpdateClusterV4RequestConverter.convert(userNamePasswordJson);
        return clusterCommonService.put(stack, updateClusterJson);
    }

    public FlowIdentifier setClusterMaintenanceMode(@NotNull NameOrCrn nameOrCrn, String accountId, @NotNull MaintenanceModeV4Request maintenanceMode) {
        StackView stack = stackDtoService.getStackViewByNameOrCrn(nameOrCrn, accountId);
        return clusterCommonService.setMaintenanceMode(stack, maintenanceMode.getStatus());
    }

    public FlowIdentifier putCluster(@NotNull String stackName, String accountId, @Valid UpdateClusterV4Request updateJson) {
        StackDto stack = stackDtoService.getByNameOrCrn(NameOrCrn.ofName(stackName), accountId);
        return clusterCommonService.put(stack, updateJson);
    }

    public String getClusterHostsInventory(Long workspaceId, String name) {
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        String loginUser = stack.getStackAuthentication().getLoginUserName();
        return clusterCommonService.getHostNamesAsIniString(stack, loginUser);
    }

    public Stack getStackByName(String name) {
        return stackService.getByNameInWorkspace(name, workspaceService.getForCurrentUser().getId());
    }

    public Stack getStackByCrn(String crn) {
        return stackService.getNotTerminatedByCrnInWorkspace(crn, workspaceService.getForCurrentUser().getId());
    }

    public List<RetryableFlow> getRetryableFlows(String name, Long workspaceId) {
        return stackCommonService.getRetryableFlows(name, workspaceId);
    }

    public FlowIdentifier backupClusterDatabase(@NotNull NameOrCrn nameOrCrn, Long workspaceId, String location, String backupId, boolean closeConnections) {
        databaseBackupRestoreService.validate(workspaceId, nameOrCrn, location, backupId);
        LOGGER.debug("Starting cluster database backup: " + nameOrCrn);
        return databaseBackupRestoreService.backupDatabase(workspaceId, nameOrCrn, location, backupId, closeConnections);
    }

    public FlowIdentifier restoreClusterDatabase(@NotNull NameOrCrn nameOrCrn, Long workspaceId, String location, String backupId) {
        databaseBackupRestoreService.validate(workspaceId, nameOrCrn, location, backupId);
        LOGGER.debug("Starting cluster database restore: " + nameOrCrn);
        return databaseBackupRestoreService.restoreDatabase(workspaceId, nameOrCrn, location, backupId);
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return stackService.getResourceCrnInTenant(resourceName, ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceName) {
        return new ArrayList<>(stackService.getResourceCrnsByNameListInTenant(resourceName, ThreadBasedUserCrnProvider.getAccountId()));
    }

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.DATAHUB;
    }

    @Override
    public Optional<String> getEnvironmentCrnByResourceCrn(String resourceCrn) {
        try {
            return Optional.of(stackService.getEnvCrnByCrn(resourceCrn));
        } catch (NotFoundException e) {
            LOGGER.error(String.format("Getting environment crn by resource crn %s failed, ", resourceCrn), e);
            return Optional.empty();
        }
    }

    @Override
    public Map<String, Optional<String>> getEnvironmentCrnsByResourceCrns(Collection<String> resourceCrns) {
        Set<String> resourceCrnSet = new LinkedHashSet<>(resourceCrns);
        List<StackCrnView> stacks = stackService.findAllByCrn(resourceCrnSet);
        Map<String, Optional<String>> resourceCrnWithEnvCrn = new LinkedHashMap<>();
        stacks.forEach(stack -> {
            resourceCrnWithEnvCrn.put(stack.getResourceCrn(), Optional.ofNullable(stack.getEnvironmentCrn()));
        });
        return resourceCrnWithEnvCrn;
    }

    public CertificatesRotationV4Response rotateAutoTlsCertificates(@NotNull NameOrCrn nameOrCrn, String accountId,
            CertificatesRotationV4Request certificatesRotationV4Request) {
        LOGGER.debug("Starting cluster autotls certificates rotation: " + nameOrCrn);
        return clusterCommonService.rotateAutoTlsCertificates(nameOrCrn, accountId, certificatesRotationV4Request);
    }

    public FlowIdentifier updateLoadBalancers(@NotNull NameOrCrn nameOrCrn, String accountId) {
        LOGGER.debug("Creating load balancers for stack: " + nameOrCrn);
        return loadBalancerUpdateService.updateLoadBalancers(nameOrCrn, accountId);
    }

    public UpdateRecipesV4Response refreshRecipes(@NotNull NameOrCrn nameOrCrn, Long workspaceId, UpdateRecipesV4Request request) {
        LOGGER.debug("Update recipes for {}", nameOrCrn);
        return clusterCommonService.refreshRecipes(nameOrCrn, workspaceId, request);
    }

    public AttachRecipeV4Response attachRecipe(@NotNull NameOrCrn nameOrCrn, Long workspaceId, AttachRecipeV4Request request) {
        LOGGER.debug("Attach recipe operation for {}", nameOrCrn);
        return clusterCommonService.attachRecipe(nameOrCrn, workspaceId, request);
    }

    public DetachRecipeV4Response detachRecipe(@NotNull NameOrCrn nameOrCrn, Long workspaceId, DetachRecipeV4Request request) {
        LOGGER.debug("Detach recipe operation for {}", nameOrCrn);
        return clusterCommonService.detachRecipe(nameOrCrn, workspaceId, request);
    }

    public RecoveryValidationV4Response validateClusterRecovery(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        return recoveryService.validateRecovery(workspaceId, nameOrCrn);
    }

    public void changeImageCatalog(@NotNull NameOrCrn nameOrCrn, Long workspaceId, String imageCatalog) {
        LOGGER.info("Updating image catalog of stack '{}' with '{}'", nameOrCrn, imageCatalog);
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        if (flowLogService.isOtherFlowRunning(stack.getId())) {
            throw new CloudbreakServiceException(String.format("Operation is running for stack '%s'. Please try again later.", stack.getName()));
        }
        stackImageService.changeImageCatalog(stack, imageCatalog);
    }

    public CloudbreakImageCatalogV3 generateImageCatalog(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        LOGGER.info("Generate image catalog of stack '{}'", nameOrCrn);
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        return generateImageCatalogService.generateImageCatalogForStack(stack);
    }

    public FlowIdentifier restartClusterServices(NameOrCrn nameOrCrn, Long workspaceId) {
        if (nameOrCrn == null) {
            throw new IllegalArgumentException("Crn must be provided.");
        }
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        return clusterOperationService.restartClusterServices(stack);
    }

    public void updateLoadBalancerDNS(Long workspaceId, NameOrCrn nameOrCrn) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        gatewayPublicEndpointManagementService.updateDnsEntryForLoadBalancers(stack);
    }
}
