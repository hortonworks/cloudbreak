package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.UserResponses.userResponses;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceV4Responses.workspaceV4Responses;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.NameComparator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.WorkspaceV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests.ChangeWorkspaceUsersV4Requests;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests.UserIds;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests.WorkspaceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.UserResponses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceV4Responses;
import com.sequenceiq.cloudbreak.api.model.users.UserIdComparator;
import com.sequenceiq.cloudbreak.api.model.users.UserResponseJson;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.NotificationController;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Controller
@Transactional(TxType.NEVER)
public class WorkspaceV4Controller extends NotificationController implements WorkspaceV4Endpoint {

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public WorkspaceV4Response post(@Valid WorkspaceV4Request workspaceV4Request) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = conversionService.convert(workspaceV4Request, Workspace.class);
        workspace = workspaceService.create(user, workspace);
        notify(ResourceEvent.WORKSPACE_CREATED);
        return conversionService.convert(workspace, WorkspaceV4Response.class);
    }

    @Override
    public WorkspaceV4Responses list() {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Set<Workspace> workspaces = workspaceService.retrieveForUser(user);
        return workspaceV4Responses(workspacesToSortedResponse(workspaces));
    }

    @Override
    public WorkspaceV4Response get(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.getByNameForUserOrThrowNotFound(name, user);
        return conversionService.convert(workspace, WorkspaceV4Response.class);
    }

    @Override
    public WorkspaceV4Response delete(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace defaultWorkspace = workspaceService.getDefaultWorkspaceForUser(user);
        Workspace workspace = workspaceService.deleteByNameForUser(name, user, defaultWorkspace);
        notify(ResourceEvent.WORKSPACE_DELETED);
        return conversionService.convert(workspace, WorkspaceV4Response.class);
    }

    @Override
    public UserResponses addUsers(String workspaceName, @Valid ChangeWorkspaceUsersV4Requests addWorkspaceUsers) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Set<User> users = workspaceService.addUsers(workspaceName, addWorkspaceUsers.getUsers(), user);
        return userResponses(usersToSortedResponse(users));
    }

    @Override
    public UserResponses changeUsers(String workspaceName, @Valid ChangeWorkspaceUsersV4Requests changeWorkspaceUsers) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Set<User> users = workspaceService.changeUsers(workspaceName, changeWorkspaceUsers.getUsers(), user);
        return userResponses(usersToSortedResponse(users));
    }

    @Override
    public UserResponses removeUsers(String workspaceName, @Valid UserIds userIds) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Set<User> users = workspaceService.removeUsers(workspaceName, userIds.getUserIds(), user);
        return userResponses(usersToSortedResponse(users));
    }

    @Override
    public UserResponses updateUsers(String workspaceName, @Valid ChangeWorkspaceUsersV4Requests updateWorkspaceUsers) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Set<User> users = workspaceService.updateUsers(workspaceName, updateWorkspaceUsers.getUsers(), user);
        return userResponses(usersToSortedResponse(users));
    }

    private SortedSet<WorkspaceV4Response> workspacesToSortedResponse(Set<Workspace> workspaces) {
        Set<WorkspaceV4Response> jsons = workspaces.stream()
                .map(o -> conversionService.convert(o, WorkspaceV4Response.class))
                .collect(Collectors.toSet());

        SortedSet<WorkspaceV4Response> sortedResponses = new TreeSet<>(new NameComparator());
        sortedResponses.addAll(jsons);
        return sortedResponses;
    }

    private SortedSet<UserResponseJson> usersToSortedResponse(Set<User> users) {
        Set<UserResponseJson> jsons = users.stream()
                .map(u -> conversionService.convert(u, UserResponseJson.class))
                .collect(Collectors.toSet());

        SortedSet<UserResponseJson> sortedResponses = new TreeSet<>(new UserIdComparator());
        sortedResponses.addAll(jsons);
        return sortedResponses;
    }

}
