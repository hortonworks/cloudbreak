package com.sequenceiq.distrox.v1.distrox;

import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.dto.StackAccessDto;
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
import com.sequenceiq.cloudbreak.service.DefaultClouderaManagerRepoService;
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
public class StackOperations {

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
    private DefaultClouderaManagerRepoService defaultClouderaManagerRepoService;

    @Inject
    private EnvironmentServiceDecorator environmentServiceDecorator;

    @Inject
    private SdxServiceDecorator sdxServiceDecorator;

    @Inject
    private UpgradeService upgradeService;

    public StackViewV4Responses listByEnvironmentName(Long workspaceId, String environmentName, StackType stackType) {
        Set<StackViewV4Response> stackViewResponses;
        LOGGER.info("List for Stack in workspace {} and environmentName {}.", workspaceId, environmentName);
        stackViewResponses = converterUtil.convertAllAsSet(
                stackApiViewService.retrieveStackViewsByWorkspaceIdAndEnvironmentName(workspaceId, environmentName, stackType),
                StackViewV4Response.class);
        LOGGER.info("Adding environment name and credential to the responses.");
        environmentServiceDecorator.prepareEnvironmentsAndCredentialName(stackViewResponses);
        LOGGER.info("Adding SDX CRN and name to the responses.");
        sdxServiceDecorator.prepareMultipleSdxAttributes(stackViewResponses);
        return new StackViewV4Responses(stackViewResponses);
    }

    public StackViewV4Responses listByEnvironmentCrn(Long workspaceId, String environmentCrn, StackType stackType) {
        Set<StackViewV4Response> stackViewResponses;
        LOGGER.info("List for Stack in workspace {} and environmentCrn {}.", workspaceId, environmentCrn);
        stackViewResponses = converterUtil.convertAllAsSet(
                stackApiViewService.retrieveStackViewsByWorkspaceIdAndEnvironmentCrn(workspaceId, environmentCrn, stackType),
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

    public StackV4Response get(@NotNull StackAccessDto stackAccessDto, Long workspaceId, Set<String> entries, StackType stackType) {
        LOGGER.info("Validate stack in workspace {}.", workspaceId);
        validateAccessDto(stackAccessDto);
        StackV4Response stackResponse;
        if (isNotEmpty(stackAccessDto.getName())) {
            stackResponse = stackCommonService.findStackByNameAndWorkspaceId(stackAccessDto.getName(), workspaceId, entries, stackType);
            LOGGER.info("Query Stack successfully finished with workspace {} name {}. Decorating environmentname and credential",
                    workspaceId, stackAccessDto.getName());
        } else {
            stackResponse = stackCommonService.findStackByCrnAndWorkspaceId(stackAccessDto.getCrn(), workspaceId, entries, stackType);
            LOGGER.info("Query Stack successfully finished with workspace {} crn {}. Decorating environmentname and credential",
                    workspaceId, stackAccessDto.getCrn());
        }
        LOGGER.info("Adding environment name and credential to the response.");
        environmentServiceDecorator.prepareEnvironmentAndCredentialName(stackResponse);
        LOGGER.info("Adding SDX CRN and name to the response.");
        sdxServiceDecorator.prepareSdxAttributes(stackResponse);
        LOGGER.info("Query Stack successfully decorated.");
        return stackResponse;
    }

    public StackViewV4Response getForInternalCrn(@NotNull StackAccessDto stackAccessDto, StackType stackType) {
        LOGGER.info("Validate stack against internal user.");
        validateAccessDto(stackAccessDto);
        StackApiView stackApiView = stackApiViewService.retrieveStackByCrnAndType(stackAccessDto.getCrn(), stackType);
        LOGGER.info("Query Stack (view) successfully finished with crn {}", stackAccessDto.getCrn());
        StackViewV4Response stackViewV4Response = converterUtil.convert(stackApiView, StackViewV4Response.class);
        LOGGER.info("Adding environment name to the response.");
        environmentServiceDecorator.prepareEnvironment(stackViewV4Response);
        return stackViewV4Response;
    }

    public void delete(StackAccessDto stackAccessDto, Long workspaceId, Boolean forced) {
        validateAccessDto(stackAccessDto);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        if (isNotEmpty(stackAccessDto.getName())) {
            LOGGER.info("Delete Stack in workspace {} with name {}.", workspaceId, stackAccessDto.getName());
            stackCommonService.deleteByNameInWorkspace(stackAccessDto.getName(), workspaceId, forced, user);
        } else {
            LOGGER.info("Delete Stack in workspace {} with crn {}.", workspaceId, stackAccessDto.getCrn());
            stackCommonService.deleteByCrnInWorkspace(stackAccessDto.getCrn(), workspaceId, forced, user);
        }
    }

    @Async
    public void asyncDelete(StackAccessDto stackAccessDto, Long workspaceId, Boolean forced) {
        validateAccessDto(stackAccessDto);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        if (isNotEmpty(stackAccessDto.getName())) {
            LOGGER.info("Delete Stack in workspace {} with name {}.", workspaceId, stackAccessDto.getName());
            stackCommonService.deleteByNameInWorkspace(stackAccessDto.getName(), workspaceId, forced, user);
        } else {
            LOGGER.info("Delete Stack in workspace {} with crn {}.", workspaceId, stackAccessDto.getCrn());
            stackCommonService.deleteByCrnInWorkspace(stackAccessDto.getCrn(), workspaceId, forced, user);
        }
    }

    public void deleteInstance(@NotNull StackAccessDto stackAccessDto, Long workspaceId, Boolean forced, String instanceId) {
        if (isNotEmpty(stackAccessDto.getName())) {
            stackCommonService.deleteInstanceByNameInWorkspace(stackAccessDto.getName(), workspaceId, instanceId, forced);
        } else {
            stackCommonService.deleteInstanceByCrnInWorkspace(stackAccessDto.getCrn(), workspaceId, instanceId, forced);
        }
    }

    public void deleteInstances(StackAccessDto stackAccessDto, Long workspaceId, List<String> instanceIds, boolean forced) {
        if (isNotEmpty(stackAccessDto.getName())) {
            stackCommonService.deleteMultipleInstancesByNameInWorkspace(stackAccessDto.getName(), workspaceId, instanceIds, forced);
        } else {
            stackCommonService.deleteMultipleInstancesByCrnInWorkspace(stackAccessDto.getCrn(), workspaceId, instanceIds, forced);
        }
    }

    public void sync(@NotNull StackAccessDto stackAccessDto, Long workspaceId) {
        if (isNotEmpty(stackAccessDto.getName())) {
            stackCommonService.syncInWorkspace(stackAccessDto.getName(), null, workspaceId);
        } else {
            stackCommonService.syncInWorkspace(null, stackAccessDto.getCrn(), workspaceId);
        }
    }

    public void retry(@NotNull StackAccessDto stackAccessDto, Long workspaceId) {
        if (isNotEmpty(stackAccessDto.getName())) {
            stackCommonService.retryInWorkspaceByName(stackAccessDto.getName(), workspaceId);
        } else {
            stackCommonService.retryInWorkspaceByCrn(stackAccessDto.getCrn(), workspaceId);
        }
    }

    public void putStop(@NotNull StackAccessDto stackAccessDto, Long workspaceId) {
        if (isNotEmpty(stackAccessDto.getName())) {
            stackCommonService.putStopInWorkspaceByName(stackAccessDto.getName(), workspaceId);
        } else {
            stackCommonService.putStopInWorkspaceByCrn(stackAccessDto.getCrn(), workspaceId);
        }
    }

    public void putStart(@NotNull StackAccessDto stackAccessDto, Long workspaceId) {
        stackCommonService.putStartInWorkspace(stackAccessDto.getName(), workspaceId);
    }

    public void putScaling(@NotNull StackAccessDto stackAccessDto, Long workspaceId, @Valid StackScaleV4Request updateRequest) {
        stackCommonService.putScalingInWorkspace(stackAccessDto.getName(), workspaceId, updateRequest);
    }

    public void repairCluster(@NotNull StackAccessDto stackAccessDto, Long workspaceId, @Valid ClusterRepairV4Request clusterRepairRequest) {
        if (isNotEmpty(stackAccessDto.getName())) {
            stackCommonService.repairClusterByName(workspaceId, stackAccessDto.getName(), clusterRepairRequest);
        } else {
            stackCommonService.repairClusterByCrn(workspaceId, stackAccessDto.getCrn(), clusterRepairRequest);
        }
    }

    public void upgradeCluster(StackAccessDto stackAccessDto, Long workspaceId) {
        if (isNotEmpty(stackAccessDto.getName())) {
            upgradeService.upgradeByStackName(workspaceId, stackAccessDto.getName());
        } else {
            throw new BadRequestException("Please provide a stack name for upgrade");
        }
    }

    public UpgradeOptionV4Response checkForUpgrade(@NotNull StackAccessDto stackAccessDto, Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        if (isNotEmpty(stackAccessDto.getName())) {
            return upgradeService.getUpgradeOptionByStackName(workspaceId, stackAccessDto.getName(), user);
        } else {
            throw new BadRequestException("Please provide a stack name for upgrade");
        }
    }

    public GeneratedBlueprintV4Response postStackForBlueprint(@NotNull StackAccessDto stackAccessDto, Long workspaceId, @Valid StackV4Request stackRequest) {
        return stackCommonService.postStackForBlueprint(stackRequest);
    }

    public void changeImage(@NotNull StackAccessDto stackAccessDto, Long workspaceId, @Valid StackImageChangeV4Request stackImageChangeRequest) {
        if (isNotEmpty(stackAccessDto.getName())) {
            stackCommonService.changeImageByNameInWorkspace(stackAccessDto.getName(), workspaceId, stackImageChangeRequest);
        } else {
            stackCommonService.changeImageByCrnInWorkspace(stackAccessDto.getCrn(), workspaceId, stackImageChangeRequest);
        }
    }

    public void deleteWithKerberos(@NotNull StackAccessDto stackAccessDto, Long workspaceId, Boolean withStackDelete) {
        if (isNotEmpty(stackAccessDto.getName())) {
            stackCommonService.deleteWithKerberosByNameInWorkspace(stackAccessDto.getName(), workspaceId, withStackDelete);
        } else {
            stackCommonService.deleteWithKerberosByCrnInWorkspace(stackAccessDto.getCrn(), workspaceId, withStackDelete);
        }
    }

    public StackV4Request getRequest(@NotNull StackAccessDto stackAccessDto, Long workspaceId) {
        if (isNotEmpty(stackAccessDto.getName())) {
            return stackService.getStackRequestByNameInWorkspaceId(stackAccessDto.getName(), workspaceId);
        } else {
            return stackService.getStackRequestByCrnInWorkspaceId(stackAccessDto.getCrn(), workspaceId);
        }
    }

    public StackStatusV4Response getStatus(@NotNull StackAccessDto stackAccessDto, Long workspaceId) {
        Stack stack;
        if (isNotEmpty(stackAccessDto.getName())) {
            stack = stackService.getByNameInWorkspace(stackAccessDto.getName(), workspaceId);
        } else {
            stack = stackService.getByCrnInWorkspace(stackAccessDto.getCrn(), workspaceId);
        }
        return converterUtil.convert(stack, StackStatusV4Response.class);
    }

    public StackStatusV4Response getStatusByCrn(@NotNull StackAccessDto stackAccessDto, Long workspaceId) {
        Stack stack;
        if (isNotEmpty(stackAccessDto.getName())) {
            stack = stackService.getByNameInWorkspace(stackAccessDto.getName(), workspaceId);
        } else {
            stack = stackService.getByCrnInWorkspace(stackAccessDto.getCrn(), workspaceId);
        }
        return converterUtil.convert(stack, StackStatusV4Response.class);
    }

    public void putPassword(@NotNull StackAccessDto stackAccessDto, Long workspaceId, @Valid UserNamePasswordV4Request userNamePasswordJson) {
        Stack stack;
        if (isNotEmpty(stackAccessDto.getName())) {
            stack = stackService.getByNameInWorkspace(stackAccessDto.getName(), workspaceId);
        } else {
            stack = stackService.getByCrnInWorkspace(stackAccessDto.getCrn(), workspaceId);
        }
        UpdateClusterV4Request updateClusterJson = converterUtil.convert(userNamePasswordJson, UpdateClusterV4Request.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        clusterCommonService.put(stack.getResourceCrn(), updateClusterJson, user, workspace);
    }

    public void setClusterMaintenanceMode(@NotNull StackAccessDto stackAccessDto, Long workspaceId, @NotNull MaintenanceModeV4Request maintenanceMode) {
        Stack stack;
        if (isNotEmpty(stackAccessDto.getName())) {
            stack = stackService.getByNameInWorkspace(stackAccessDto.getName(), workspaceId);
        } else {
            stack = stackService.getByCrnInWorkspace(stackAccessDto.getCrn(), workspaceId);
        }
        clusterCommonService.setMaintenanceMode(stack, maintenanceMode.getStatus());
    }

    public void putCluster(@NotNull StackAccessDto stackAccessDto, Long workspaceId, @Valid UpdateClusterV4Request updateJson) {
        Stack stack;
        if (isNotEmpty(stackAccessDto.getName())) {
            stack = stackService.getByNameInWorkspace(stackAccessDto.getName(), workspaceId);
        } else {
            stack = stackService.getByCrnInWorkspace(stackAccessDto.getCrn(), workspaceId);
        }
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        clusterCommonService.put(stack.getResourceCrn(), updateJson, user, workspace);
    }

    public String getClusterHostsInventory(Long workspaceId, String name) {
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        String loginUser = stack.getStackAuthentication().getLoginUserName();
        return clusterCommonService.getHostNamesAsIniString(stack.getCluster(), loginUser);
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

    private void validateAccessDto(StackAccessDto dto) {
        throwIfNull(dto, () -> new IllegalArgumentException("StackAccessDto should not be null."));
        if (dto.isNotValid()) {
            throw new BadRequestException("A stack name or crn must be provided. One and only one of them.");
        }
    }
}
