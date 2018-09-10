package com.sequenceiq.cloudbreak.api.model.users;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.validation.ValidChangeWorkspaceUsersJson;

import io.swagger.annotations.ApiModel;

@ValidChangeWorkspaceUsersJson
@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeWorkspaceUsersJson {

    private String userId;

    private Set<String> permissions;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
