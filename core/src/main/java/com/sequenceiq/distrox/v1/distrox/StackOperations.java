package com.sequenceiq.distrox.v1.distrox;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.ResourceAccessDto;
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

    public StackV4Response get(ResourceAccessDto resourceAccessDto, Long workspaceId, Set<String> entries, StackType stackType) {
        LOGGER.info("Validate stack in workspace {}.", workspaceId);
        ResourceAccessDto.validate(resourceAccessDto);
        StackV4Response stackResponse;
        if (isNotEmpty(resourceAccessDto.getName())) {
            stackResponse = stackCommonService.findStackByNameAndWorkspaceId(resourceAccessDto.getName(), workspaceId, entries, stackType);
            LOGGER.info("Query Stack successfully finished with workspace {} name {}. Decorating environmentname and credential",
                    workspaceId, resourceAccessDto.getName());
        } else {
            stackResponse = stackCommonService.findStackByCrnAndWorkspaceId(resourceAccessDto.getCrn(), workspaceId, entries, stackType);
            LOGGER.info("Query Stack successfully finished with workspace {} crn {}. Decorating environmentname and credential",
                    workspaceId, resourceAccessDto.getCrn());
        }
        LOGGER.info("Adding environment name and credential to the response.");
        environmentServiceDecorator.prepareEnvironmentAndCredentialName(stackResponse);
        LOGGER.info("Adding SDX CRN and name to the response.");
        sdxServiceDecorator.prepareSdxAttributes(stackResponse);
        LOGGER.info("Query Stack successfully decorated.");
        return stackResponse;
    }

    public StackViewV4Response getForInternalCrn(ResourceAccessDto resourceAccessDto, StackType stackType) {
        LOGGER.info("Validate stack against internal user.");
        ResourceAccessDto.validate(resourceAccessDto);
        StackApiView stackApiView = stackApiViewService.retrieveStackByCrnAndType(resourceAccessDto.getCrn(), stackType);
        LOGGER.info("Query Stack (view) successfully finished with crn {}", resourceAccessDto.getCrn());
        StackViewV4Response stackViewV4Response = converterUtil.convert(stackApiView, StackViewV4Response.class);
        LOGGER.info("Adding environment name to the response.");
        environmentServiceDecorator.prepareEnvironment(stackViewV4Response);
        return stackViewV4Response;
    }

    public void deleteInstance(@NotNull ResourceAccessDto resourceAccessDto, Long workspaceId, boolean forced, String instanceId) {
        if (isNotEmpty(resourceAccessDto.getName())) {
            stackCommonService.deleteInstanceByNameInWorkspace(resourceAccessDto.getName(), workspaceId, instanceId, forced);
        } else {
            stackCommonService.deleteInstanceByCrnInWorkspace(resourceAccessDto.getCrn(), workspaceId, instanceId, forced);
        }
    }

    public void deleteInstances(ResourceAccessDto resourceAccessDto, Long workspaceId, List<String> instanceIds, boolean forced) {
        if (isNotEmpty(resourceAccessDto.getName())) {
            stackCommonService.deleteMultipleInstancesByNameInWorkspace(resourceAccessDto.getName(), workspaceId, instanceIds, forced);
        } else {
            stackCommonService.deleteMultipleInstancesByCrnInWorkspace(resourceAccessDto.getCrn(), workspaceId, instanceIds, forced);
        }
    }

    public void sync(ResourceAccessDto resourceAccessDto, Long workspaceId) {
        if (isNotEmpty(resourceAccessDto.getName())) {
            stackCommonService.syncInWorkspace(resourceAccessDto.getName(), null, workspaceId);
        } else {
            stackCommonService.syncInWorkspace(null, resourceAccessDto.getCrn(), workspaceId);
        }
    }

    public void retry(ResourceAccessDto resourceAccessDto, Long workspaceId) {
        ResourceAccessDto.validate(resourceAccessDto);
        if (isNotEmpty(resourceAccessDto.getName())) {
            stackCommonService.retryInWorkspaceByName(resourceAccessDto.getName(), workspaceId);
        } else {
            stackCommonService.retryInWorkspaceByCrn(resourceAccessDto.getCrn(), workspaceId);
        }
    }

    public void putStop(ResourceAccessDto resourceAccessDto, Long workspaceId) {
        if (isNotEmpty(resourceAccessDto.getName())) {
            stackCommonService.putStopInWorkspaceByName(resourceAccessDto.getName(), workspaceId);
        } else {
            stackCommonService.putStopInWorkspaceByCrn(resourceAccessDto.getCrn(), workspaceId);
        }
    }

    public void putStart(ResourceAccessDto resourceAccessDto, Long workspaceId) {
        if (isNotEmpty(resourceAccessDto.getName())) {
            stackCommonService.putStartInWorkspaceByName(resourceAccessDto.getName(), workspaceId);
        } else {
            stackCommonService.putStartInWorkspaceByCrn(resourceAccessDto.getCrn(), workspaceId);
        }
    }

    public void putScaling(@NotNull ResourceAccessDto resourceAccessDto, Long workspaceId, @Valid StackScaleV4Request updateRequest) {
        stackCommonService.putScalingInWorkspace(resourceAccessDto.getName(), workspaceId, updateRequest);
    }

    public void repairCluster(@NotNull ResourceAccessDto resourceAccessDto, Long workspaceId, @Valid ClusterRepairV4Request clusterRepairRequest) {
        if (isNotEmpty(resourceAccessDto.getName())) {
            stackCommonService.repairClusterByName(workspaceId, resourceAccessDto.getName(), clusterRepairRequest);
        } else {
            stackCommonService.repairClusterByCrn(workspaceId, resourceAccessDto.getCrn(), clusterRepairRequest);
        }
    }

    public void upgradeCluster(@NotNull ResourceAccessDto resourceAccessDto, Long workspaceId) {
        if (isNotEmpty(resourceAccessDto.getName())) {
            upgradeService.upgradeByStackName(workspaceId, resourceAccessDto.getName());
        } else {
            throw new BadRequestException("Please provide a stack name for upgrade");
        }
    }

    public UpgradeOptionV4Response checkForUpgrade(@NotNull ResourceAccessDto resourceAccessDto, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        if (isNotEmpty(resourceAccessDto.getName())) {
            return upgradeService.getUpgradeOptionByStackName(workspaceId, resourceAccessDto.getName(), user);
        } else {
            throw new BadRequestException("Please provide a stack name for upgrade");
        }
    }

    public GeneratedBlueprintV4Response postStackForBlueprint(ResourceAccessDto resourceAccessDto, Long workspaceId, @Valid StackV4Request stackRequest) {
        return stackCommonService.postStackForBlueprint(stackRequest);
    }

    public void changeImage(@NotNull ResourceAccessDto resourceAccessDto, Long workspaceId, @Valid StackImageChangeV4Request stackImageChangeRequest) {
        if (isNotEmpty(resourceAccessDto.getName())) {
            stackCommonService.changeImageByNameInWorkspace(resourceAccessDto.getName(), workspaceId, stackImageChangeRequest);
        } else {
            stackCommonService.changeImageByCrnInWorkspace(resourceAccessDto.getCrn(), workspaceId, stackImageChangeRequest);
        }
    }

    public void delete(@NotNull ResourceAccessDto resourceAccessDto, Long workspaceId, boolean forced) {
        ResourceAccessDto.validate(resourceAccessDto);
        if (isNotEmpty(resourceAccessDto.getName())) {
            stackCommonService.deleteWithKerberosByNameInWorkspace(resourceAccessDto.getName(), workspaceId, forced);
        } else {
            stackCommonService.deleteWithKerberosByCrnInWorkspace(resourceAccessDto.getCrn(), workspaceId, forced);
        }
    }

    public StackV4Request getRequest(@NotNull ResourceAccessDto resourceAccessDto, Long workspaceId) {
        if (isNotEmpty(resourceAccessDto.getName())) {
            return stackService.getStackRequestByNameInWorkspaceId(resourceAccessDto.getName(), workspaceId);
        } else {
            return stackService.getStackRequestByCrnInWorkspaceId(resourceAccessDto.getCrn(), workspaceId);
        }
    }

    public StackStatusV4Response getStatus(@NotNull ResourceAccessDto resourceAccessDto, Long workspaceId) {
        Stack stack;
        if (isNotEmpty(resourceAccessDto.getName())) {
            stack = stackService.getByNameInWorkspace(resourceAccessDto.getName(), workspaceId);
        } else {
            stack = stackService.getByCrnInWorkspace(resourceAccessDto.getCrn(), workspaceId);
        }
        return converterUtil.convert(stack, StackStatusV4Response.class);
    }

    public StackStatusV4Response getStatusByCrn(@NotNull ResourceAccessDto resourceAccessDto, Long workspaceId) {
        Stack stack;
        if (isNotEmpty(resourceAccessDto.getName())) {
            stack = stackService.getByNameInWorkspace(resourceAccessDto.getName(), workspaceId);
        } else {
            stack = stackService.getByCrnInWorkspace(resourceAccessDto.getCrn(), workspaceId);
        }
        return converterUtil.convert(stack, StackStatusV4Response.class);
    }

    public StackStatusV4Response getStatus(@NotNull String crn) {
        Stack stack = stackService.getByCrn(crn);
        return converterUtil.convert(stack, StackStatusV4Response.class);
    }

    public void putPassword(@NotNull ResourceAccessDto resourceAccessDto, Long workspaceId, @Valid UserNamePasswordV4Request userNamePasswordJson) {
        Stack stack;
        if (isNotEmpty(resourceAccessDto.getName())) {
            stack = stackService.getByNameInWorkspace(resourceAccessDto.getName(), workspaceId);
        } else {
            stack = stackService.getByCrnInWorkspace(resourceAccessDto.getCrn(), workspaceId);
        }
        UpdateClusterV4Request updateClusterJson = converterUtil.convert(userNamePasswordJson, UpdateClusterV4Request.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        clusterCommonService.put(stack.getResourceCrn(), updateClusterJson, user, workspace);
    }

    public void setClusterMaintenanceMode(@NotNull ResourceAccessDto resourceAccessDto, Long workspaceId, @NotNull MaintenanceModeV4Request maintenanceMode) {
        Stack stack;
        if (isNotEmpty(resourceAccessDto.getName())) {
            stack = stackService.getByNameInWorkspace(resourceAccessDto.getName(), workspaceId);
        } else {
            stack = stackService.getByCrnInWorkspace(resourceAccessDto.getCrn(), workspaceId);
        }
        clusterCommonService.setMaintenanceMode(stack, maintenanceMode.getStatus());
    }

    public void putCluster(@NotNull ResourceAccessDto resourceAccessDto, Long workspaceId, @Valid UpdateClusterV4Request updateJson) {
        Stack stack;
        if (isNotEmpty(resourceAccessDto.getName())) {
            stack = stackService.getByNameInWorkspace(resourceAccessDto.getName(), workspaceId);
        } else {
            stack = stackService.getByCrnInWorkspace(resourceAccessDto.getCrn(), workspaceId);
        }
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
