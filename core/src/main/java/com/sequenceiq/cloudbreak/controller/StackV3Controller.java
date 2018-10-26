package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.StackV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.GeneratedBlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.MaintenanceModeJson;
import com.sequenceiq.cloudbreak.api.model.ReinstallRequestV2;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackImageChangeRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackScaleRequestV2;
import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRepairRequest;
import com.sequenceiq.cloudbreak.api.model.users.UserNamePasswordJson;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
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
@Transactional(TxType.NEVER)
@WorkspaceEntityType(Stack.class)
public class StackV3Controller extends NotificationController implements StackV3Endpoint {

    @Inject
    private StackCommonService stackCommonService;

    @Inject
    private ClusterCommonService clusterCommonService;

    @Inject
    private StackService stackService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Override
    public Set<StackViewResponse> listByWorkspace(Long workspaceId) {
        return stackCommonService.retrieveStacksByWorkspaceId(workspaceId);
    }

    @Override
    public StackResponse getByNameInWorkspace(Long workspaceId, String name, Set<String> entries) {
        return stackCommonService.findStackByNameAndWorkspaceId(name, workspaceId, entries);
    }

    @Override
    public StackResponse createInWorkspace(Long workspaceId, StackV2Request request) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(workspaceId, user);
        return stackCommonService.createInWorkspace(conversionService.convert(request, StackRequest.class), cloudbreakUser, user, workspace);
    }

    @Override
    public void deleteInWorkspace(Long workspaceId, String name, Boolean forced, Boolean deleteDependencies) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        stackCommonService.deleteInWorkspace(name, workspaceId, forced, deleteDependencies, user);
    }

    @Override
    public Response putSyncInWorkspace(Long workspaceId, String name) {
        return stackCommonService.putSyncInWorkspace(name, workspaceId);
    }

    @Override
    public void retryInWorkspace(Long workspaceId, String name) {
        stackCommonService.retryInWorkspace(name, workspaceId);
    }

    @Override
    public Response putStopInWorkspace(Long workspaceId, String name) {
        return stackCommonService.putStopInWorkspace(name, workspaceId);
    }

    @Override
    public Response putStartInWorkspace(Long workspaceId, String name) {
        return stackCommonService.putStartInWorkspace(name, workspaceId);
    }

    @Override
    public Response putScalingInWorkspace(Long workspaceId, String name, StackScaleRequestV2 updateRequest) {
        return stackCommonService.putScalingInWorkspace(name, workspaceId, updateRequest);
    }

    @Override
    public Response repairClusterInWorkspace(Long workspaceId, String name, ClusterRepairRequest clusterRepairRequest) {
        stackCommonService.repairCluster(workspaceId, name, clusterRepairRequest);
        return Response.accepted().build();
    }

    @Override
    public void deleteWithKerberosInWorkspace(Long workspaceId, String name, Boolean withStackDelete, Boolean deleteDependencies) {
        stackCommonService.deleteWithKerbereosInWorkspace(name, workspaceId, withStackDelete, deleteDependencies);
    }

    @Override
    public StackV2Request getRequestfromName(Long workspaceId, String name) {
        return stackService.getStackRequestByNameInWorkspaceId(name, workspaceId);
    }

    @Override
    public GeneratedBlueprintResponse postStackForBlueprint(Long workspaceId, String name, StackV2Request stackRequest) {
        return stackCommonService.postStackForBlueprint(stackRequest);
    }

    @Override
    public Response deleteInstance(Long workspaceId, String name, String instanceId, boolean forced) {
        return stackCommonService.deleteInstanceByNameInWorkspace(name, workspaceId, instanceId, forced);
    }

    @Override
    public Response changeImage(Long workspaceId, String name, StackImageChangeRequest stackImageChangeRequest) {
        return stackCommonService.changeImageByNameInWorkspace(name, workspaceId, stackImageChangeRequest);
    }

    @Override
    public Response putReinstall(Long workspaceId, String name, ReinstallRequestV2 reinstallRequestV2) {
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        UpdateClusterJson updateClusterJson = conversionService.convert(reinstallRequestV2, UpdateClusterJson.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return clusterCommonService.put(stack.getId(), updateClusterJson, user, workspace);
    }

    @Override
    public Response putPassword(Long workspaceId, String name, @Valid UserNamePasswordJson userNamePasswordJson) {
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        UpdateClusterJson updateClusterJson = conversionService.convert(userNamePasswordJson, UpdateClusterJson.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return clusterCommonService.put(stack.getId(), updateClusterJson, user, workspace);
    }

    @Override
    public Map<String, Object> getStatusByNameInWorkspace(Long workspaceId, String name) {
        return stackService.getStatusByNameInWorkspace(name, workspaceId);
    }

    @Override
    public Response setClusterMaintenanceMode(Long workspaceId, String name, @NotNull MaintenanceModeJson maintenanceMode) {
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        return clusterCommonService.setMaintenanceMode(stack, maintenanceMode.getStatus());
    }

    @Override
    public Response put(Long workspaceId, String name, UpdateClusterJson updateJson) {
        Stack stack = stackService.getByNameInWorkspace(name, workspaceId);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return clusterCommonService.put(stack.getId(), updateJson, user, workspace);
    }
}
