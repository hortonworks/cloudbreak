package com.sequenceiq.distrox.v1.distrox;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.cdp.shaded.org.apache.commons.lang3.StringUtils;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.ResourceBasedCrnProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.MaintenanceModeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.projection.StackClusterStatusView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.retry.RetryableFlow;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;
import com.sequenceiq.cloudbreak.service.DatabaseBackupRestoreService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.stack.StackApiViewService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeAvailabilityService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.distrox.v1.distrox.service.EnvironmentServiceDecorator;
import com.sequenceiq.distrox.v1.distrox.service.SdxServiceDecorator;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class StackOperations implements ResourceBasedCrnProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackOperations.class);

    @Inject
    private StackCommonService stackCommonService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterCommonService clusterCommonService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private StackApiViewService stackApiViewService;

    @Inject
    private EnvironmentServiceDecorator environmentServiceDecorator;

    @Inject
    private SdxServiceDecorator sdxServiceDecorator;

    @Inject
    private UpgradeService upgradeService;

    @Inject
    private ClusterUpgradeAvailabilityService clusterUpgradeAvailabilityService;

    @Inject
    private DatabaseBackupRestoreService databaseBackupRestoreService;

    public StackViewV4Responses listByEnvironmentName(Long workspaceId, String environmentName, List<StackType> stackTypes) {
        Set<StackViewV4Response> stackViewResponses;
        LOGGER.info("List for Stack in workspace {} and environmentName {}.", workspaceId, environmentName);
        stackViewResponses = converterUtil.convertAllAsSet(
                stackApiViewService.retrieveStackViewsByWorkspaceIdAndEnvironmentName(workspaceId, environmentName, stackTypes),
                StackViewV4Response.class);
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
        stackViewResponses = converterUtil.convertAllAsSet(
                stackApiViewService.retrieveStackViewsByWorkspaceIdAndEnvironmentCrn(workspaceId, environmentCrn, stackTypes),
                StackViewV4Response.class);
        LOGGER.info("Adding environment name and credential to the responses.");
        NameOrCrn nameOrCrn = StringUtils.isEmpty(environmentCrn) ? NameOrCrn.empty() : NameOrCrn.ofCrn(environmentCrn);
        environmentServiceDecorator.prepareEnvironmentsAndCredentialName(stackViewResponses, nameOrCrn);
        LOGGER.info("Adding SDX CRN and name to the responses.");
        sdxServiceDecorator.prepareMultipleSdxAttributes(stackViewResponses);
        return new StackViewV4Responses(stackViewResponses);
    }

    public StackV4Response post(Long workspaceId, @Valid StackV4Request request, boolean distroxRequest) {
        LOGGER.info("Post for Stack in workspace {}.", workspaceId);
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
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

    public StackV4Response get(NameOrCrn nameOrCrn, Long workspaceId, Set<String> entries, StackType stackType) {
        LOGGER.info("Validate stack in workspace {}.", workspaceId);
        StackV4Response stackResponse = stackCommonService.findStackByNameOrCrnAndWorkspaceId(nameOrCrn, workspaceId, entries, stackType);
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
        StackViewV4Response stackViewV4Response = converterUtil.convert(stackApiView, StackViewV4Response.class);
        LOGGER.info("Adding environment name to the response.");
        environmentServiceDecorator.prepareEnvironment(stackViewV4Response);
        return stackViewV4Response;
    }

    public FlowIdentifier deleteInstance(@NotNull NameOrCrn nameOrCrn, Long workspaceId, boolean forced, String instanceId) {
        return stackCommonService.deleteInstanceInWorkspace(nameOrCrn, workspaceId, instanceId, forced);
    }

    public FlowIdentifier deleteInstances(NameOrCrn nameOrCrn, Long workspaceId, List<String> instanceIds, boolean forced) {
        return stackCommonService.deleteMultipleInstancesInWorkspace(nameOrCrn, workspaceId, instanceIds, forced);
    }

    public FlowIdentifier sync(NameOrCrn nameOrCrn, Long workspaceId) {
        return stackCommonService.syncInWorkspace(nameOrCrn, workspaceId);
    }

    public FlowIdentifier retry(NameOrCrn nameOrCrn, Long workspaceId) {
        return stackCommonService.retryInWorkspace(nameOrCrn, workspaceId);
    }

    public FlowIdentifier putStop(NameOrCrn nameOrCrn, Long workspaceId) {
        return stackCommonService.putStopInWorkspace(nameOrCrn, workspaceId);
    }

    public FlowIdentifier putStart(NameOrCrn nameOrCrn, Long workspaceId) {
        return stackCommonService.putStartInWorkspace(nameOrCrn, workspaceId);
    }

    public FlowIdentifier putScaling(@NotNull NameOrCrn nameOrCrn, Long workspaceId, @Valid StackScaleV4Request updateRequest) {
        return stackCommonService.putScalingInWorkspace(nameOrCrn, workspaceId, updateRequest);
    }

    public FlowIdentifier repairCluster(@NotNull NameOrCrn nameOrCrn, Long workspaceId, @Valid ClusterRepairV4Request clusterRepairRequest) {
        LOGGER.debug("Starting to repair cluster with request: {}", clusterRepairRequest.toString());
        return stackCommonService.repairCluster(workspaceId, nameOrCrn, clusterRepairRequest);
    }

    public FlowIdentifier upgradeOs(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        LOGGER.debug("Starting to upgrade OS: " + nameOrCrn);
        return upgradeService.upgradeOs(workspaceId, nameOrCrn);
    }

    public UpgradeOptionV4Response checkForOsUpgrade(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        if (nameOrCrn.hasName()) {
            return upgradeService.getOsUpgradeOptionByStackNameOrCrn(workspaceId, nameOrCrn, user);
        } else {
            LOGGER.debug("No stack name provided for upgrade, found: " + nameOrCrn);
            throw new BadRequestException("Please provide a stack name for upgrade");
        }
    }

    public FlowIdentifier upgradeCluster(@NotNull NameOrCrn nameOrCrn, Long workspaceId, String imageId) {
        LOGGER.debug("Starting to upgrade cluster: " + nameOrCrn);
        return upgradeService.upgradeCluster(workspaceId, nameOrCrn, imageId);
    }

    public FlowIdentifier updateSalt(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        LOGGER.debug("Starting salt update: " + nameOrCrn);
        return clusterCommonService.updateSalt(nameOrCrn, workspaceId);
    }

    public FlowIdentifier updatePillarConfiguration(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        LOGGER.debug("Starting pillar configuration update: " + nameOrCrn);
        return clusterCommonService.updatePillarConfiguration(nameOrCrn, workspaceId);
    }

    public UpgradeV4Response checkForClusterUpgrade(@NotNull NameOrCrn nameOrCrn, Long workspaceId, UpgradeV4Request request) {
        if (nameOrCrn.hasName()) {
            String stackName = nameOrCrn.getName();
            boolean osUpgrade = upgradeService.isOsUpgrade(request);
            UpgradeV4Response upgradeResponse = clusterUpgradeAvailabilityService.checkForUpgradesByName(workspaceId, stackName,
                    osUpgrade);
            clusterUpgradeAvailabilityService.filterUpgradeOptions(upgradeResponse, request);
            Stack stack = getStackByName(stackName);
            MDCBuilder.buildMdcContext(stack);
            StackViewV4Responses stackViewV4Responses = listByEnvironmentCrn(workspaceId, stack.getEnvironmentCrn(), List.of(StackType.WORKLOAD));
            clusterUpgradeAvailabilityService.checkForNotAttachedClusters(stackViewV4Responses, upgradeResponse);
            if (!osUpgrade && !request.isDryRun() && !request.isShowAvailableImagesSet()) {
                clusterUpgradeAvailabilityService.checkIfClusterRuntimeUpgradable(workspaceId, stackName, upgradeResponse);
            }
            return upgradeResponse;
        } else {
            LOGGER.debug("No stack name provided for upgrade, found: " + nameOrCrn);
            throw new BadRequestException("Please provide a stack name for upgrade");
        }
    }

    public GeneratedBlueprintV4Response postStackForBlueprint(@Valid StackV4Request stackRequest) {
        return stackCommonService.postStackForBlueprint(stackRequest);
    }

    public FlowIdentifier changeImage(@NotNull NameOrCrn nameOrCrn, Long workspaceId, @Valid StackImageChangeV4Request stackImageChangeRequest) {
        return stackCommonService.changeImageInWorkspace(nameOrCrn, workspaceId, stackImageChangeRequest);
    }

    public void delete(@NotNull NameOrCrn nameOrCrn, Long workspaceId, boolean forced) {
        stackCommonService.deleteWithKerberosInWorkspace(nameOrCrn, workspaceId, forced);
    }

    public StackV4Request getRequest(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        return stackService.getStackRequestByNameOrCrnInWorkspaceId(nameOrCrn, workspaceId);
    }

    public StackStatusV4Response getStatus(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        StackClusterStatusView stackStatusView = stackService.getStatusByNameOrCrn(nameOrCrn, workspaceId);
        return converterUtil.convert(stackStatusView, StackStatusV4Response.class);
    }

    public StackStatusV4Response getStatusByCrn(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        StackClusterStatusView stackStatusView = stackService.getStatusByNameOrCrn(nameOrCrn, workspaceId);
        return converterUtil.convert(stackStatusView, StackStatusV4Response.class);
    }

    public StackStatusV4Response getStatus(@NotNull String crn) {
        StackClusterStatusView stackStatusView = stackService.getStatusByCrn(crn);
        return converterUtil.convert(stackStatusView, StackStatusV4Response.class);
    }

    public FlowIdentifier putPassword(@NotNull NameOrCrn nameOrCrn, Long workspaceId, @Valid UserNamePasswordV4Request userNamePasswordJson) {
        Stack stack = nameOrCrn.hasName()
                ? stackService.getByNameInWorkspace(nameOrCrn.getName(), workspaceId)
                : stackService.getByCrnInWorkspace(nameOrCrn.getCrn(), workspaceId);
        UpdateClusterV4Request updateClusterJson = converterUtil.convert(userNamePasswordJson, UpdateClusterV4Request.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return clusterCommonService.put(stack.getResourceCrn(), updateClusterJson);
    }

    public FlowIdentifier setClusterMaintenanceMode(@NotNull NameOrCrn nameOrCrn, Long workspaceId, @NotNull MaintenanceModeV4Request maintenanceMode) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        return clusterCommonService.setMaintenanceMode(stack, maintenanceMode.getStatus());
    }

    public FlowIdentifier putCluster(@NotNull NameOrCrn nameOrCrn, Long workspaceId, @Valid UpdateClusterV4Request updateJson) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return clusterCommonService.put(stack.getResourceCrn(), updateJson);
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
        return stackService.getByCrnInWorkspace(crn, workspaceService.getForCurrentUser().getId());
    }

    public List<RetryableFlow> getRetryableFlows(String name, Long workspaceId) {
        return stackCommonService.getRetryableFlows(name, workspaceId);
    }

    public FlowIdentifier backupClusterDatabase(@NotNull NameOrCrn nameOrCrn, Long workspaceId, String location, String backupId) {
        databaseBackupRestoreService.validate(workspaceId, nameOrCrn, location, backupId);
        LOGGER.debug("Starting cluster database backup: " + nameOrCrn);
        return databaseBackupRestoreService.backupDatabase(workspaceId, nameOrCrn, location, backupId);
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
    public List<String> getResourceCrnsInAccount() {
        return new ArrayList<>(stackService.getResourceCrnsByTenant(ThreadBasedUserCrnProvider.getAccountId()));
    }

    @Override
    public AuthorizationResourceType getResourceType() {
        return AuthorizationResourceType.DATAHUB;
    }
}
