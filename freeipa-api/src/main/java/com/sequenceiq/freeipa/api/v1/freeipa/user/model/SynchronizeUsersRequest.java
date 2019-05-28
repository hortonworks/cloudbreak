package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("SynchronizeUsersV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SynchronizeUsersRequest {
    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_ENVIRONMENTS)
    private Set<String> environments = new HashSet<>();

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_USERS)
    private Set<String> users = new HashSet<>();

    public Set<String> getEnvironments() {
        return environments;
    }

    public Set<String> getUsers() {
        return users;
    }
}
