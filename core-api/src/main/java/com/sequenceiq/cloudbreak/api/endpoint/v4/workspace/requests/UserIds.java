package com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests;

import java.util.Set;

import javax.validation.Valid;

import io.swagger.annotations.ApiModel;

@ApiModel
public class UserIds {

    @Valid
    private Set<String> userIds;

    public Set<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(Set<String> userIds) {
        this.userIds = userIds;
    }
}
