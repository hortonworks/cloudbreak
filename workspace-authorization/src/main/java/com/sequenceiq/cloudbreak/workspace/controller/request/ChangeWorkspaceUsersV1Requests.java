package com.sequenceiq.cloudbreak.workspace.controller.request;

import java.util.Set;

import javax.validation.Valid;

import io.swagger.annotations.ApiModel;

@ApiModel
public class ChangeWorkspaceUsersV1Requests {

    @Valid
    private Set<ChangeWorkspaceUsersV1Request> users;

    public Set<ChangeWorkspaceUsersV1Request> getUsers() {
        return users;
    }

    public void setUsers(Set<ChangeWorkspaceUsersV1Request> users) {
        this.users = users;
    }

    public static final ChangeWorkspaceUsersV1Requests changeWorkspaceUsersV4Requests(Set<ChangeWorkspaceUsersV1Request> users) {
        ChangeWorkspaceUsersV1Requests changeWorkspaceUsersV4Requests = new ChangeWorkspaceUsersV1Requests();
        changeWorkspaceUsersV4Requests.users = users;
        return changeWorkspaceUsersV4Requests;
    }
}
