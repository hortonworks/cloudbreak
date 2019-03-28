package com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.authorization.WorkspaceRole;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeWorkspaceUsersV4Request {

    @NotNull
    private String userId;

    //@NotNull
    private Set<WorkspaceRole> roles;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Set<WorkspaceRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<WorkspaceRole> roles) {
        this.roles = roles;
    }
}
