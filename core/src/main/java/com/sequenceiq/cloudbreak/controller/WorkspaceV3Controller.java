package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
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
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v3.WorkspaceV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.users.ChangeWorkspaceUsersJson;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceRequest;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResponse.NameComparator;
import com.sequenceiq.cloudbreak.api.model.users.UserResponseJson;
import com.sequenceiq.cloudbreak.api.model.users.UserResponseJson.UserIdComparator;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
@Transactional(TxType.NEVER)
public class WorkspaceV3Controller extends NotificationController implements WorkspaceV3Endpoint {

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
    public WorkspaceResponse create(@Valid WorkspaceRequest workspaceRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Workspace workspace = conversionService.convert(workspaceRequest, Workspace.class);
        workspace = workspaceService.create(user, workspace);
        notify(ResourceEvent.WORKSPACE_CREATED);
        return conversionService.convert(workspace, WorkspaceResponse.class);
    }

    @Override
    public SortedSet<WorkspaceResponse> getAll() {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Set<Workspace> workspaces = workspaceService.retrieveForUser(user);
        return workspacesToSortedResponse(workspaces);
    }

    @Override
    public WorkspaceResponse getByName(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Workspace workspace = workspaceService.getByNameForUserOrThrowNotFound(name, user);
        return conversionService.convert(workspace, WorkspaceResponse.class);
    }

    @Override
    public WorkspaceResponse deleteByName(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Workspace defaultWorkspace = workspaceService.getDefaultWorkspaceForUser(user);
        Workspace workspace = workspaceService.deleteByNameForUser(name, user, defaultWorkspace);
        notify(ResourceEvent.WORKSPACE_DELETED);
        return conversionService.convert(workspace, WorkspaceResponse.class);
    }

    @Override
    public SortedSet<UserResponseJson> addUsers(String orgName, @Valid Set<ChangeWorkspaceUsersJson> addWorkspaceUsersJson) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Set<User> users = workspaceService.addUsers(orgName, addWorkspaceUsersJson, user);
        return usersToSortedResponse(users);
    }

    @Override
    public SortedSet<UserResponseJson> changeUsers(String orgName, @Valid Set<ChangeWorkspaceUsersJson> changeWorkspaceUsersJson) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Set<User> users = workspaceService.changeUsers(orgName, jsonToMap(changeWorkspaceUsersJson), user);
        return usersToSortedResponse(users);
    }

    @Override
    public SortedSet<UserResponseJson> removeUsers(String orgName, @Valid Set<String> userIds) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Set<User> users = workspaceService.removeUsers(orgName, userIds, user);
        return usersToSortedResponse(users);
    }

    @Override
    public SortedSet<UserResponseJson> updateUsers(String orgName, @Valid Set<ChangeWorkspaceUsersJson> updateWorkspaceUsersJson) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Set<User> users = workspaceService.updateUsers(orgName, updateWorkspaceUsersJson, user);
        return usersToSortedResponse(users);
    }

    private Map<String, Set<String>> jsonToMap(Set<ChangeWorkspaceUsersJson> changeWorkspaceUsersJsons) {
        return changeWorkspaceUsersJsons.stream()
                .collect(Collectors.toMap(ChangeWorkspaceUsersJson::getUserId, ChangeWorkspaceUsersJson::getPermissions));
    }

    private SortedSet<WorkspaceResponse> workspacesToSortedResponse(Set<Workspace> workspaces) {
        Set<WorkspaceResponse> jsons = workspaces.stream()
                .map(o -> conversionService.convert(o, WorkspaceResponse.class))
                .collect(Collectors.toSet());

        SortedSet<WorkspaceResponse> sortedResponses = new TreeSet<>(new NameComparator());
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
