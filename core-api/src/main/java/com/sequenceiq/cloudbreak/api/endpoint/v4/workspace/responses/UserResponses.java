package com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.WorkspaceV4Base;
import com.sequenceiq.cloudbreak.api.model.users.UserResponseJson;

import io.swagger.annotations.ApiModel;

@ApiModel
public class UserResponses extends WorkspaceV4Base {

    private Set<UserResponseJson> users;

    public Set<UserResponseJson> getUsers() {
        return users;
    }

    public void setUsers(Set<UserResponseJson> users) {
        this.users = users;
    }

    public static final UserResponses userResponses(Set<UserResponseJson> users) {
        UserResponses userResponses = new UserResponses();
        userResponses.setUsers(users);
        return userResponses;
    }

}
