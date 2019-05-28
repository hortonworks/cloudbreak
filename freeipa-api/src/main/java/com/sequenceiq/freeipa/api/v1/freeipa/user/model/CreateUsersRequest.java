package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("CreateUsersV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateUsersRequest {
    @NotNull
    @ApiModelProperty(value = FreeIpaModelDescriptions.ENVIRONMENT_ID, required = true)
    private String environmentId;

    @ApiModelProperty(value = UserModelDescriptions.USERCREATE_GROUPS)
    private Set<Group> groups = new HashSet<>();

    @ApiModelProperty(value = UserModelDescriptions.USERCREATE_USERS)
    private Set<User> users = new HashSet<>();

    public String getEnvironmentId() {
        return environmentId;
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public Set<User> getUsers() {
        return users;
    }
}
