package com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests;

import java.util.Set;

import javax.validation.Valid;

import io.swagger.annotations.ApiModel;

@ApiModel
public class ChangeWorkspaceUsersV4Requests {

    @Valid
    private Set<ChangeWorkspaceUsersV4Request> users;

    public Set<ChangeWorkspaceUsersV4Request> getUsers() {
        return users;
    }

    public void setUsers(Set<ChangeWorkspaceUsersV4Request> users) {
        this.users = users;
    }

    public static final ChangeWorkspaceUsersV4Requests changeWorkspaceUsersV4Requests(Set<ChangeWorkspaceUsersV4Request> users) {
        ChangeWorkspaceUsersV4Requests changeWorkspaceUsersV4Requests = new ChangeWorkspaceUsersV4Requests();
        changeWorkspaceUsersV4Requests.users = users;
        return changeWorkspaceUsersV4Requests;
    }
}
