package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UsersyncModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("SynchronizeUsersV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SynchronizeUsersRequest {
    @NotNull
    @ApiModelProperty(value = UsersyncModelDescriptions.ENVIRONMENT_NAME, required = true)
    private String environmentName;

    // TODO: figure out whether we need a separate name since we are expecting one freeipa per environment.
    // We currently retrieve stacks from the repository by environment and name and then
    // get the freeipa from the stack.
    @NotNull
    @ApiModelProperty(value = UsersyncModelDescriptions.STACK_NAME, required = true)
    private String name;

    @ApiModelProperty(value = UsersyncModelDescriptions.USERSYNC_GROUPS)
    private Set<Group> groups = new HashSet<>();

    @ApiModelProperty(value = UsersyncModelDescriptions.USERSYNC_USERS)
    private Set<User> users = new HashSet<>();

    public String getEnvironmentName() {
        return environmentName;
    }

    public String getName() {
        return name;
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public Set<User> getUsers() {
        return users;
    }
}
