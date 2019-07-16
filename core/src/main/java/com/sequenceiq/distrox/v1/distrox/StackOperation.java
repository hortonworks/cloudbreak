package com.sequenceiq.distrox.v1.distrox;

import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;
import com.sequenceiq.cloudbreak.service.DefaultClouderaManagerRepoService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.stack.StackApiViewService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.distrox.v1.distrox.service.EnvironmentServiceDecorator;

@Service
public class StackOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackOperation.class);

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

    public StackViewV4Responses listByEnvironmentName(Long workspaceId, String environmentName, StackType stackType) {
        Set<StackViewV4Response> stackViewResponses;
        LOGGER.info("List for Stack in workspace {} and environmentName {}.", workspaceId, environmentName);
        stackViewResponses = converterUtil.convertAllAsSet(
                stackApiViewService.retrieveStackViewsByWorkspaceIdAndEnvironmentName(workspaceId, environmentName, stackType),
                StackViewV4Response.class);
        LOGGER.info("Adding environment name and credential to the responses.");
        environmentServiceDecorator.prepareEnvironmentsAndCredentialName(stackViewResponses);
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
        return new StackViewV4Responses(stackViewResponses);
    }

    public StackV4Response post(Long workspaceId, @Valid StackV4Request request) {
        LOGGER.info("Post for Stack in workspace {}.", workspaceId);
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        LOGGER.info("Cloudbreak user for the requested stack is {}.", cloudbreakUser);
        Workspace workspace = workspaceService.get(workspaceId, user);
        StackV4Response stackV4Response = stackCommonService.createInWorkspace(request, user, workspace);
        LOGGER.info("Adding environment name and credential to the responses.");
        environmentServiceDecorator.prepareEnvironmentAndCredentialName(stackV4Response);
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
        environmentServiceDecorator.prepareEnvironmentAndCredentialName(stackResponse);
        LOGGER.info("Query Stack successfully decorated.");
        return stackResponse;
    }

    public void delete(StackAccessDto stackAccessDto, Long workspaceId, Boolean forced, Boolean deleteDependencies) {
        validateAccessDto(stackAccessDto);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        if (isNotEmpty(stackAccessDto.getName())) {
            LOGGER.info("Delete Stack in workspace {} with name {}.", workspaceId, stackAccessDto.getName());
            stackCommonService.deleteByNameInWorkspace(stackAccessDto.getName(), workspaceId, forced, deleteDependencies, user);
        } else {
            LOGGER.info("Delete Stack in workspace {} with crn {}.", workspaceId, stackAccessDto.getCrn());
            stackCommonService.deleteByCrnInWorkspace(stackAccessDto.getCrn(), workspaceId, forced, deleteDependencies, user);
        }
    }

    public void putSync(Long workspaceId, String name) {
        LOGGER.info("Sync Stack in workspace {} with name {}.", workspaceId, name);
        stackCommonService.putSyncInWorkspace(name, workspaceId);
    }

    public void putRetry(Long workspaceId, String name) {
        LOGGER.info("Retry Stack in workspace {} with name {}.", workspaceId, name);
        stackCommonService.retryInWorkspace(name, workspaceId);
    }

    public void putStop(Long workspaceId, String name) {
        LOGGER.info("Stop Stack in workspace {} with name {}.", workspaceId, name);
        stackCommonService.putStopInWorkspace(name, workspaceId);
    }

    public void putStart(Long workspaceId, String name) {
        LOGGER.info("Put Stack in workspace {} with name {}.", workspaceId, name);
        stackCommonService.putStartInWorkspace(name, workspaceId);
    }

    public void putScaling(Long workspaceId, String name, @Valid StackScaleV4Request updateRequest) {
        LOGGER.info("Scaling Stack in workspace {} with name {}.", workspaceId, name);
        stackCommonService.putScalingInWorkspace(name, workspaceId, updateRequest);
    }

    public void repairCluster(Long workspaceId, String name, @Valid ClusterRepairV4Request clusterRepairRequest) {
        LOGGER.info("Repair Stack in workspace {} with name {}.", workspaceId, name);
        stackCommonService.repairCluster(workspaceId, name, clusterRepairRequest);
    }

    public GeneratedBlueprintV4Response postStackForBlueprint(Long workspaceId, String name, @Valid StackV4Request stackRequest) {
        LOGGER.info("Generate blueprint for Stack in workspace {} with name {}.", workspaceId, name);
        return stackCommonService.postStackForBlueprint(stackRequest);
    }

    public void changeImage(Long workspaceId, String name, @Valid StackImageChangeV4Request stackImageChangeRequest) {
        LOGGER.info("Change image for Stack in workspace {} with name {}.", workspaceId, name);
        stackCommonService.changeImageByNameInWorkspace(name, workspaceId, stackImageChangeRequest);
    }

    public void deleteWithKerberos(Long workspaceId, String name, Boolean withStackDelete, Boolean deleteDependencies) {
        LOGGER.info("Delete with Kerberos for Stack in workspace {} with name {}.", workspaceId, name);
        stackCommonService.deleteWithKerbereosInWorkspace(name, workspaceId, withStackDelete, deleteDependencies);
    }

    public StackV4Request getRequestfromName(Long workspaceId, String name) {
        LOGGER.info("Get request for Stack in workspace {} with name {}.", workspaceId, name);
        return stackService.getStackRequestByNameInWorkspaceId(name, workspaceId);
    }

    public StackStatusV4Response getStatusByName(Long workspaceId, String name) {
        LOGGER.info("Get status for Stack in workspace {} with name {}.", workspaceId, name);
        return converterUtil.convert(stackService.getByNameInWorkspace(name, workspaceId), StackStatusV4Response.class);
    }

    public void deleteInstance(Long workspaceId, String name, Boolean forced, String instanceId) {
        LOGGER.info("Delete instance for Stack in workspace {} with name {}.", workspaceId, name);
        stackCommonService.deleteInstanceByNameInWorkspace(name, workspaceId, instanceId, forced);
    }

    public void putPassword(Long workspaceId, String name, @Valid UserNamePasswordV4Request userNamePasswordJson) {
        LOGGER.info("Put Stack password in workspace {} with name {}.", workspaceId, name);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        UpdateClusterV4Request updateClusterJson = converterUtil.convert(userNamePasswordJson, UpdateClusterV4Request.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        clusterCommonService.put(stack.getResourceCrn(), updateClusterJson, user, workspace);
    }

    public void setClusterMaintenanceMode(Long workspaceId, String name, @NotNull MaintenanceModeV4Request maintenanceMode) {
        LOGGER.info("Put Stack maintenance mode in workspace {} with name {}.", workspaceId, name);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        clusterCommonService.setMaintenanceMode(stack, maintenanceMode.getStatus());
    }

    public void putCluster(Long workspaceId, String name, @Valid UpdateClusterV4Request updateJson) {
        LOGGER.info("Put Cluster in workspace {} with name {}.", workspaceId, name);
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        clusterCommonService.put(stack.getResourceCrn(), updateJson, user, workspace);
    }

    public Response getClusterHostsInventory(Long workspaceId, String name) {
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        String iniStr = clusterCommonService.getHostNamesAsIniString(stack.getCluster());
        return Response
                .ok(iniStr, MediaType.APPLICATION_OCTET_STREAM)
                .header("content-disposition", String.format("attachment; filename = %s-hosts.ini", stack.getName()))
                .build();
    }

    public Stack getStackByName(String name) {
        return stackService.getByNameInWorkspace(name, workspaceService.getForCurrentUser().getId());
    }

    private void validateAccessDto(StackAccessDto dto) {
        throwIfNull(dto, () -> new IllegalArgumentException("StackAccessDto should not be null"));
        if (dto.isNotValid()) {
            throw new BadRequestException("A stack name or crn must be provided. One and only one of them.");
        }
    }

}
