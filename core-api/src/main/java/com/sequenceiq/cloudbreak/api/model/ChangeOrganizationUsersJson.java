package com.sequenceiq.cloudbreak.api.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.validation.ValidChangeOrganizationUsersJson;

import io.swagger.annotations.ApiModel;

@ValidChangeOrganizationUsersJson
@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeOrganizationUsersJson {

    private String userName;

    private Set<String> permissions;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
