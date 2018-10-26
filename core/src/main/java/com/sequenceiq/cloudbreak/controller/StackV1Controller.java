package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.StackV1Endpoint;
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackValidationRequest;
import com.sequenceiq.cloudbreak.authorization.PermissionCheckerService;
import com.sequenceiq.cloudbreak.authorization.PermissionCheckingUtils;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Controller
@Transactional(TxType.NEVER)
public class StackV1Controller extends NotificationController implements StackV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV1Controller.class);

    @Autowired
    private StackCreatorService stackCreatorService;

    @Autowired
    private StackCommonService stackCommonService;

    @Inject
    private StackService stackService;

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private PermissionCheckerService permissionCheckerService;

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Override
    public StackResponse postPrivate(StackRequest stackRequest) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return stackCommonService.createInWorkspace(stackRequest, cloudbreakUser, user, workspace);
    }

    @Override
    public StackResponse postPublic(StackRequest stackRequest) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return stackCommonService.createInWorkspace(stackRequest, cloudbreakUser, user, workspace);
    }

    @Override
    public Set<StackResponse> getStacksInDefaultWorkspace() {
        return stackCommonService.getStacksInDefaultWorkspace();
    }

    @Override
    public Set<StackResponse> getPublics() {
        return stackCommonService.getStacksInDefaultWorkspace();
    }

    @Override
    public StackResponse getStackFromDefaultWorkspace(String name, Set<String> entries) {
        return stackCommonService.getStackFromDefaultWorkspace(name, entries);
    }

    @Override
    public StackResponse getPublic(String name, Set<String> entries) {
        return stackCommonService.getStackFromDefaultWorkspace(name, entries);
    }

    @Override
    public StackResponse get(Long id, Set<String> entries) {
        return stackCommonService.get(id, entries);
    }

    @Override
    public void deleteInDefaultWorkspace(String name, Boolean forced, Boolean deleteDependencies) {
        stackCommonService.deleteInDefaultWorkspace(name, forced, deleteDependencies);
    }

    @Override
    public void deletePrivate(String name, Boolean forced, Boolean deleteDependencies) {
        stackCommonService.deleteInDefaultWorkspace(name, forced, deleteDependencies);
    }

    @Override
    public void deleteById(Long id, Boolean forced, Boolean deleteDependencies) {
        stackCommonService.deleteById(id, forced, deleteDependencies);
    }

    @Override
    public Response put(Long id, UpdateStackJson updateRequest) {
        return stackCommonService.putInDefaultWorkspace(id, updateRequest);
    }

    @Override
    public Map<String, Object> status(Long id) {
        return stackCommonService.status(id);
    }

    @Override
    public PlatformVariantsJson variants() {
        return stackCommonService.variants();
    }

    @Override
    public Response deleteInstance(Long stackId, String instanceId) {
        return stackCommonService.deleteInstance(stackId, instanceId, false);
    }

    @Override
    public Response deleteInstance(Long stackId, String instanceId, boolean forced) {
        return stackCommonService.deleteInstance(stackId, instanceId, forced);
    }

    @Override
    public Response deleteInstances(Long stackId, Set<String> instanceIds) {
        return stackCommonService.deleteInstances(stackId, instanceIds);
    }

    @Override
    public Response validate(StackValidationRequest stackValidationRequest) {
        return stackCommonService.validate(stackValidationRequest);
    }
}
