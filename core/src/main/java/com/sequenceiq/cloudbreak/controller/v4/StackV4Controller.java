package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.MaintenanceModeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ReinstallV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedClusterDefinitionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@WorkspaceEntityType(Stack.class)
public class StackV4Controller extends NotificationController implements StackV4Endpoint {

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

    @Override
    public StackViewV4Responses list(Long workspaceId, String environment, Boolean onlyDatalakes) {
        Set<StackViewV4Response> stackViewResponses = stackCommonService.retrieveStacksByWorkspaceId(workspaceId, environment, onlyDatalakes);
        return new StackViewV4Responses(stackViewResponses);
    }

    @Override
    public StackV4Response post(Long workspaceId, @Valid StackV4Request request) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(workspaceId, user);
        return stackCommonService.createInWorkspace(request, cloudbreakUser, user, workspace);
    }

    @Override
    public StackV4Response get(Long workspaceId, String name, Set<String> entries) {
        return stackCommonService.findStackByNameAndWorkspaceId(name, workspaceId, entries);
    }

    @Override
    public void delete(Long workspaceId, String name, Boolean forced, Boolean deleteDependencies) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        stackCommonService.deleteInWorkspace(name, workspaceId, forced, deleteDependencies, user);
    }

    @Override
    public void putSync(Long workspaceId, String name) {
        stackCommonService.putSyncInWorkspace(name, workspaceId);
    }

    @Override
    public void putRetry(Long workspaceId, String name) {
        stackCommonService.retryInWorkspace(name, workspaceId);
    }

    @Override
    public void putStop(Long workspaceId, String name) {
        stackCommonService.putStopInWorkspace(name, workspaceId);
    }

    @Override
    public void putStart(Long workspaceId, String name) {
        stackCommonService.putStartInWorkspace(name, workspaceId);
    }

    @Override
    public void putScaling(Long workspaceId, String name, @Valid StackScaleV4Request updateRequest) {
        stackCommonService.putScalingInWorkspace(name, workspaceId, updateRequest);
    }

    @Override
    public void repairCluster(Long workspaceId, String name, @Valid ClusterRepairV4Request clusterRepairRequest) {
        stackCommonService.repairCluster(workspaceId, name, clusterRepairRequest);
    }

    @Override
    public GeneratedClusterDefinitionV4Response postStackForClusterDefinition(Long workspaceId, String name, @Valid StackV4Request stackRequest) {
        return stackCommonService.postStackForClusterDefinition(stackRequest);
    }

    @Override
    public void changeImage(Long workspaceId, String name, @Valid StackImageChangeV4Request stackImageChangeRequest) {
        stackCommonService.changeImageByNameInWorkspace(name, workspaceId, stackImageChangeRequest);
    }

    @Override
    public void deleteWithKerberos(Long workspaceId, String name, Boolean withStackDelete, Boolean deleteDependencies) {
        stackCommonService.deleteWithKerbereosInWorkspace(name, workspaceId, withStackDelete, deleteDependencies);
    }

    @Override
    public StackV4Request getRequestfromName(Long workspaceId, String name) {
        return stackService.getStackRequestByNameInWorkspaceId(name, workspaceId);
    }

    @Override
    public StackStatusV4Response getStatusByName(Long workspaceId, String name) {
        return converterUtil.convert(stackService.getByNameInWorkspace(name, workspaceId), StackStatusV4Response.class);
    }

    @Override
    public void deleteInstance(Long workspaceId, String name, Boolean forced, String instanceId) {
        stackCommonService.deleteInstanceByNameInWorkspace(name, workspaceId, instanceId, forced);
    }

    @Override
    public void putReinstall(Long workspaceId, String name, @Valid ReinstallV4Request reinstallRequest) {
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        UpdateClusterV4Request updateCluster = converterUtil.convert(reinstallRequest, UpdateClusterV4Request.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        clusterCommonService.put(stack.getId(), updateCluster, user, workspace);
    }

    @Override
    public void putPassword(Long workspaceId, String name, @Valid UserNamePasswordV4Request userNamePasswordJson) {
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        UpdateClusterV4Request updateClusterJson = converterUtil.convert(userNamePasswordJson, UpdateClusterV4Request.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        clusterCommonService.put(stack.getId(), updateClusterJson, user, workspace);
    }

    @Override
    public void setClusterMaintenanceMode(Long workspaceId, String name, @NotNull MaintenanceModeV4Request maintenanceMode) {
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        clusterCommonService.setMaintenanceMode(stack, maintenanceMode.getStatus());
    }

    @Override
    public void putCluster(Long workspaceId, String name, @Valid UpdateClusterV4Request updateJson) {
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        clusterCommonService.put(stack.getId(), updateJson, user, workspace);
    }
}
