package com.sequenceiq.distrox.v1.distrox;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.ResourceBasedEnvironmentCrnProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.MaintenanceModeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.retry.RetryableFlow;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.stack.StackApiViewService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.distrox.v1.distrox.service.EnvironmentServiceDecorator;
import com.sequenceiq.distrox.v1.distrox.service.SdxServiceDecorator;

@Service
public class StackOperations implements ResourceBasedEnvironmentCrnProvider {

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

    public StackViewV4Responses listByEnvironmentName(Long workspaceId, String environmentName, List<StackType> stackTypes) {
        Set<StackViewV4Response> stackViewResponses;
        LOGGER.info("List for Stack in workspace {} and environmentName {}.", workspaceId, environmentName);
        stackViewResponses = converterUtil.convertAllAsSet(
                stackApiViewService.retrieveStackViewsByWorkspaceIdAndEnvironmentName(workspaceId, environmentName, stackTypes),
                StackViewV4Response.class);
        LOGGER.info("Adding environment name and credential to the responses.");
        environmentServiceDecorator.prepareEnvironmentsAndCredentialName(stackViewResponses);
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
        environmentServiceDecorator.prepareEnvironmentsAndCredentialName(stackViewResponses);
        LOGGER.info("Adding SDX CRN and name to the responses.");
        sdxServiceDecorator.prepareMultipleSdxAttributes(stackViewResponses);
        return new StackViewV4Responses(stackViewResponses);
    }

    public StackV4Response post(Long workspaceId, @Valid StackV4Request request) {
        LOGGER.info("Post for Stack in workspace {}.", workspaceId);
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        LOGGER.info("Cloudbreak user for the requested stack is {}.", cloudbreakUser);
        Workspace workspace = workspaceService.get(workspaceId, user);
        StackV4Response stackV4Response = stackCommonService.createInWorkspace(request, user, workspace);
        LOGGER.info("Adding environment name and credential to the response.");
        environmentServiceDecorator.prepareEnvironmentAndCredentialName(stackV4Response);
        LOGGER.info("Adding SDX CRN and name to the response.");
        sdxServiceDecorator.prepareSdxAttributes(stackV4Response);
        return stackV4Response;
    }

    public StackV4Response get(NameOrCrn nameOrCrn, Long workspaceId, Set<String> entries, StackType stackType) {
        LOGGER.info("Validate stack in workspace {}.", workspaceId);
        StackV4Response stackResponse;
        if (nameOrCrn.hasName()) {
            stackResponse = stackCommonService.findStackByNameAndWorkspaceId(nameOrCrn.getName(), workspaceId, entries, stackType);
            LOGGER.info("Query Stack successfully finished with workspace {} name {}. Decorating environmentname and credential",
                    workspaceId, nameOrCrn);
        } else {
            stackResponse = stackCommonService.findStackByCrnAndWorkspaceId(nameOrCrn.getCrn(), workspaceId, entries, stackType);
            LOGGER.info("Query Stack successfully finished with workspace {} crn {}. Decorating environmentname and credential",
                    workspaceId, nameOrCrn);
        }
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

    public void deleteInstance(@NotNull NameOrCrn nameOrCrn, Long workspaceId, boolean forced, String instanceId) {
        if (nameOrCrn.hasName()) {
            stackCommonService.deleteInstanceByNameInWorkspace(nameOrCrn.getName(), workspaceId, instanceId, forced);
        } else {
            stackCommonService.deleteInstanceByCrnInWorkspace(nameOrCrn.getCrn(), workspaceId, instanceId, forced);
        }
    }

    public void deleteInstances(NameOrCrn nameOrCrn, Long workspaceId, List<String> instanceIds, boolean forced) {
        if (nameOrCrn.hasName()) {
            stackCommonService.deleteMultipleInstancesByNameInWorkspace(nameOrCrn.getName(), workspaceId, instanceIds, forced);
        } else {
            stackCommonService.deleteMultipleInstancesByCrnInWorkspace(nameOrCrn.getCrn(), workspaceId, instanceIds, forced);
        }
    }

    public void sync(NameOrCrn nameOrCrn, Long workspaceId) {
        if (nameOrCrn.hasName()) {
            stackCommonService.syncInWorkspace(nameOrCrn.getName(), null, workspaceId);
        } else {
            stackCommonService.syncInWorkspace(null, nameOrCrn.getCrn(), workspaceId);
        }
    }

    public void retry(NameOrCrn nameOrCrn, Long workspaceId) {
        if (nameOrCrn.hasName()) {
            stackCommonService.retryInWorkspaceByName(nameOrCrn.getName(), workspaceId);
        } else {
            stackCommonService.retryInWorkspaceByCrn(nameOrCrn.getCrn(), workspaceId);
        }
    }

    public void putStop(NameOrCrn nameOrCrn, Long workspaceId) {
        if (nameOrCrn.hasName()) {
            stackCommonService.putStopInWorkspaceByName(nameOrCrn.getName(), workspaceId);
        } else {
            stackCommonService.putStopInWorkspaceByCrn(nameOrCrn.getCrn(), workspaceId);
        }
    }

    public void putStart(NameOrCrn nameOrCrn, Long workspaceId) {
        if (nameOrCrn.hasName()) {
            stackCommonService.putStartInWorkspaceByName(nameOrCrn.getName(), workspaceId);
        } else {
            stackCommonService.putStartInWorkspaceByCrn(nameOrCrn.getCrn(), workspaceId);
        }
    }

    public void putScaling(@NotNull NameOrCrn nameOrCrn, Long workspaceId, @Valid StackScaleV4Request updateRequest) {
        stackCommonService.putScalingInWorkspace(nameOrCrn.getName(), workspaceId, updateRequest);
    }

    public void repairCluster(@NotNull NameOrCrn nameOrCrn, Long workspaceId, @Valid ClusterRepairV4Request clusterRepairRequest) {
        if (nameOrCrn.hasName()) {
            stackCommonService.repairClusterByName(workspaceId, nameOrCrn.getName(), clusterRepairRequest);
        } else {
            stackCommonService.repairClusterByCrn(workspaceId, nameOrCrn.getCrn(), clusterRepairRequest);
        }
    }

    public void upgradeCluster(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        if (nameOrCrn.hasName()) {
            upgradeService.upgradeByStackName(workspaceId, nameOrCrn.getName());
        } else {
            LOGGER.debug("No stack name provided for upgrade, found: " + nameOrCrn);
            throw new BadRequestException("Please provide a stack name for upgrade");
        }
    }

    public UpgradeOptionV4Response checkForUpgrade(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        if (nameOrCrn.hasName()) {
            return upgradeService.getUpgradeOptionByStackName(workspaceId, nameOrCrn.getName(), user);
        } else {
            LOGGER.debug("No stack name provided for upgrade, found: " + nameOrCrn);
            throw new BadRequestException("Please provide a stack name for upgrade");
        }
    }

    public GeneratedBlueprintV4Response postStackForBlueprint(NameOrCrn nameOrCrn, Long workspaceId, @Valid StackV4Request stackRequest) {
        return stackCommonService.postStackForBlueprint(stackRequest);
    }

    public void changeImage(@NotNull NameOrCrn nameOrCrn, Long workspaceId, @Valid StackImageChangeV4Request stackImageChangeRequest) {
        if (nameOrCrn.hasName()) {
            stackCommonService.changeImageByNameInWorkspace(nameOrCrn.getName(), workspaceId, stackImageChangeRequest);
        } else {
            stackCommonService.changeImageByCrnInWorkspace(nameOrCrn.getCrn(), workspaceId, stackImageChangeRequest);
        }
    }

    public void delete(@NotNull NameOrCrn nameOrCrn, Long workspaceId, boolean forced) {
        if (nameOrCrn.hasName()) {
            stackCommonService.deleteWithKerberosByNameInWorkspace(nameOrCrn.getName(), workspaceId, forced);
        } else {
            stackCommonService.deleteWithKerberosByCrnInWorkspace(nameOrCrn.getCrn(), workspaceId, forced);
        }
    }

    public StackV4Request getRequest(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        return nameOrCrn.hasName()
                ? stackService.getStackRequestByNameInWorkspaceId(nameOrCrn.getName(), workspaceId)
                : stackService.getStackRequestByCrnInWorkspaceId(nameOrCrn.getCrn(), workspaceId);
    }

    public StackStatusV4Response getStatus(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        Stack stack = nameOrCrn.hasName()
                ? stackService.getByNameInWorkspace(nameOrCrn.getName(), workspaceId)
                : stackService.getByCrnInWorkspace(nameOrCrn.getCrn(), workspaceId);
        return converterUtil.convert(stack, StackStatusV4Response.class);
    }

    public StackStatusV4Response getStatusByCrn(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        Stack stack = nameOrCrn.hasName()
                ? stackService.getByNameInWorkspace(nameOrCrn.getName(), workspaceId)
                : stackService.getByCrnInWorkspace(nameOrCrn.getCrn(), workspaceId);
        return converterUtil.convert(stack, StackStatusV4Response.class);
    }

    public StackStatusV4Response getStatus(@NotNull String crn) {
        Stack stack = stackService.getByCrn(crn);
        return converterUtil.convert(stack, StackStatusV4Response.class);
    }

    public void putPassword(@NotNull NameOrCrn nameOrCrn, Long workspaceId, @Valid UserNamePasswordV4Request userNamePasswordJson) {
        Stack stack = nameOrCrn.hasName()
                ? stackService.getByNameInWorkspace(nameOrCrn.getName(), workspaceId)
                : stackService.getByCrnInWorkspace(nameOrCrn.getCrn(), workspaceId);
        UpdateClusterV4Request updateClusterJson = converterUtil.convert(userNamePasswordJson, UpdateClusterV4Request.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        clusterCommonService.put(stack.getResourceCrn(), updateClusterJson, user, workspace);
    }

    public void setClusterMaintenanceMode(@NotNull NameOrCrn nameOrCrn, Long workspaceId, @NotNull MaintenanceModeV4Request maintenanceMode) {
        Stack stack = nameOrCrn.hasName()
                ? stackService.getByNameInWorkspace(nameOrCrn.getName(), workspaceId)
                : stackService.getByCrnInWorkspace(nameOrCrn.getCrn(), workspaceId);
        clusterCommonService.setMaintenanceMode(stack, maintenanceMode.getStatus());
    }

    public void putCluster(@NotNull NameOrCrn nameOrCrn, Long workspaceId, @Valid UpdateClusterV4Request updateJson) {
        Stack stack = nameOrCrn.hasName()
                ? stackService.getByNameInWorkspace(nameOrCrn.getName(), workspaceId)
                : stackService.getByCrnInWorkspace(nameOrCrn.getCrn(), workspaceId);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        clusterCommonService.put(stack.getResourceCrn(), updateJson, user, workspace);
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

    @Override
    public String getEnvironmentCrnByResourceName(String resourceName) {
        return stackService.getViewByNameInWorkspace(resourceName, workspaceService.getForCurrentUser().getId()).getEnvironmentCrn();
    }

    @Override
    public String getEnvironmentCrnByResourceCrn(String resourceCrn) {
        return stackService.getViewByCrnInWorkspace(resourceCrn, workspaceService.getForCurrentUser().getId()).getEnvironmentCrn();
    }

    @Override
    public List<String> getEnvironmentCrnListByResourceCrnList(List<String> resourceCrns) {
        return stackService.getViewsByCrnListInWorkspace(resourceCrns, workspaceService.getForCurrentUser().getId())
                .stream()
                .map(view -> view.getEnvironmentCrn())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getEnvironmentCrnListByResourceNameList(List<String> resourceName) {
        return stackService.getViewsByNameListInWorkspace(resourceName, workspaceService.getForCurrentUser().getId())
                .stream()
                .map(view -> view.getEnvironmentCrn())
                .collect(Collectors.toList());
    }
}
