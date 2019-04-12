package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.validation.Valid;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.NameComparator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.UserIdComparator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.WorkspaceV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests.ChangeWorkspaceUsersV4Requests;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests.UserIds;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests.WorkspaceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.UserV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.UserV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceV4Responses;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

//@Controller
//@Transactional(TxType.NEVER)
public class WorkspaceV4Controller extends NotificationController implements WorkspaceV4Endpoint {

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public WorkspaceV4Response post(@Valid WorkspaceV4Request workspaceV4Request) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = converterUtil.convert(workspaceV4Request, Workspace.class);
        workspace = workspaceService.create(user, workspace);
        notify(ResourceEvent.WORKSPACE_CREATED, false, Collections.singleton(workspace.getId()));
        return converterUtil.convert(workspace, WorkspaceV4Response.class);
    }

    @Override
    public WorkspaceV4Responses list() {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Set<Workspace> workspaces = workspaceService.retrieveForUser(user);
        return new WorkspaceV4Responses(workspacesToSortedResponse(workspaces));
    }

    @Override
    public WorkspaceV4Response get(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.getByNameForUserOrThrowNotFound(name, user);
        return converterUtil.convert(workspace, WorkspaceV4Response.class);
    }

    @Override
    public WorkspaceV4Response delete(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace defaultWorkspace = workspaceService.getDefaultWorkspaceForUser(user);
        Workspace workspace = workspaceService.deleteByNameForUser(name, user, defaultWorkspace);
        notify(ResourceEvent.WORKSPACE_DELETED, false, Collections.singleton(workspace.getId()));
        return converterUtil.convert(workspace, WorkspaceV4Response.class);
    }

    @Override
    public UserV4Responses addUsers(String workspaceName, @Valid ChangeWorkspaceUsersV4Requests addWorkspaceUsers) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Set<User> users = workspaceService.addUsers(workspaceName, addWorkspaceUsers.getUsers(), user);
        return new UserV4Responses(usersToSortedResponse(users));
    }

    @Override
    public UserV4Responses changeUsers(String workspaceName, @Valid ChangeWorkspaceUsersV4Requests changeWorkspaceUsers) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Set<User> users = workspaceService.changeUsers(workspaceName, changeWorkspaceUsers.getUsers(), user);
        return new UserV4Responses(usersToSortedResponse(users));
    }

    @Override
    public UserV4Responses removeUsers(String workspaceName, @Valid UserIds userIds) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Set<User> users = workspaceService.removeUsers(workspaceName, userIds.getUserIds(), user);
        return new UserV4Responses(usersToSortedResponse(users));
    }

    @Override
    public UserV4Responses updateUsers(String workspaceName, @Valid ChangeWorkspaceUsersV4Requests updateWorkspaceUsers) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Set<User> users = workspaceService.updateUsers(workspaceName, updateWorkspaceUsers.getUsers(), user);
        return new UserV4Responses(usersToSortedResponse(users));
    }

    private SortedSet<WorkspaceV4Response> workspacesToSortedResponse(Set<Workspace> workspaces) {
        Set<WorkspaceV4Response> jsons = converterUtil.convertAllAsSet(workspaces, WorkspaceV4Response.class);
        SortedSet<WorkspaceV4Response> sortedResponses = new TreeSet<>(new NameComparator());
        sortedResponses.addAll(jsons);
        return sortedResponses;
    }

    private SortedSet<UserV4Response> usersToSortedResponse(Set<User> users) {
        Set<UserV4Response> jsons = converterUtil.convertAllAsSet(users, UserV4Response.class);
        SortedSet<UserV4Response> sortedResponses = new TreeSet<>(new UserIdComparator());
        sortedResponses.addAll(jsons);
        return sortedResponses;
    }

}
