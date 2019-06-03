package com.sequenceiq.distrox.v1.distrox;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Service;

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

@Service
public class StackOperation {

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

    public StackViewV4Responses list(Long workspaceId, String environmentCrn, Boolean onlyDatalakes) {
        Set<StackViewV4Response> stackViewResponses = converterUtil.convertAllAsSet(
                stackApiViewService.retrieveStackViewsByWorkspaceId(workspaceId, environmentCrn, onlyDatalakes),
                StackViewV4Response.class
        );
        return new StackViewV4Responses(stackViewResponses);
    }

    public StackV4Response post(Long workspaceId, @Valid StackV4Request request) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(workspaceId, user);
        return stackCommonService.createInWorkspace(request, user, workspace);
    }

    public StackV4Response get(Long workspaceId, String name, Set<String> entries) {
        return stackCommonService.findStackByNameAndWorkspaceId(name, workspaceId, entries);
    }

    public void delete(Long workspaceId, String name, Boolean forced, Boolean deleteDependencies) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        stackCommonService.deleteInWorkspace(name, workspaceId, forced, deleteDependencies, user);
    }

    public void putSync(Long workspaceId, String name) {
        stackCommonService.putSyncInWorkspace(name, workspaceId);
    }

    public void putRetry(Long workspaceId, String name) {
        stackCommonService.retryInWorkspace(name, workspaceId);
    }

    public void putStop(Long workspaceId, String name) {
        stackCommonService.putStopInWorkspace(name, workspaceId);
    }

    public void putStart(Long workspaceId, String name) {
        stackCommonService.putStartInWorkspace(name, workspaceId);
    }

    public void putScaling(Long workspaceId, String name, @Valid StackScaleV4Request updateRequest) {
        stackCommonService.putScalingInWorkspace(name, workspaceId, updateRequest);
    }

    public void repairCluster(Long workspaceId, String name, @Valid ClusterRepairV4Request clusterRepairRequest) {
        stackCommonService.repairCluster(workspaceId, name, clusterRepairRequest);
    }

    public GeneratedBlueprintV4Response postStackForBlueprint(Long workspaceId, String name, @Valid StackV4Request stackRequest) {
        return stackCommonService.postStackForBlueprint(stackRequest);
    }

    public void changeImage(Long workspaceId, String name, @Valid StackImageChangeV4Request stackImageChangeRequest) {
        stackCommonService.changeImageByNameInWorkspace(name, workspaceId, stackImageChangeRequest);
    }

    public void deleteWithKerberos(Long workspaceId, String name, Boolean withStackDelete, Boolean deleteDependencies) {
        stackCommonService.deleteWithKerbereosInWorkspace(name, workspaceId, withStackDelete, deleteDependencies);
    }

    public StackV4Request getRequestfromName(Long workspaceId, String name) {
        return stackService.getStackRequestByNameInWorkspaceId(name, workspaceId);
    }

    public StackStatusV4Response getStatusByName(Long workspaceId, String name) {
        return converterUtil.convert(stackService.getByNameInWorkspace(name, workspaceId), StackStatusV4Response.class);
    }

    public void deleteInstance(Long workspaceId, String name, Boolean forced, String instanceId) {
        stackCommonService.deleteInstanceByNameInWorkspace(name, workspaceId, instanceId, forced);
    }

    public void putPassword(Long workspaceId, String name, @Valid UserNamePasswordV4Request userNamePasswordJson) {
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        UpdateClusterV4Request updateClusterJson = converterUtil.convert(userNamePasswordJson, UpdateClusterV4Request.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        clusterCommonService.put(stack.getId(), updateClusterJson, user, workspace);
    }

    public void setClusterMaintenanceMode(Long workspaceId, String name, @NotNull MaintenanceModeV4Request maintenanceMode) {
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        clusterCommonService.setMaintenanceMode(stack, maintenanceMode.getStatus());
    }

    public void putCluster(Long workspaceId, String name, @Valid UpdateClusterV4Request updateJson) {
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        clusterCommonService.put(stack.getId(), updateJson, user, workspace);
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
}
