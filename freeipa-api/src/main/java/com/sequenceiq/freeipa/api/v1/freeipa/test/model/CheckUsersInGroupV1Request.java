package com.sequenceiq.freeipa.api.v1.freeipa.test.model;

import java.util.Set;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("CheckUsersInGroupV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckUsersInGroupV1Request extends ClientTestBaseRequest {

    @NotEmpty
    @ApiModelProperty(value = ModelDescriptions.USERS, required = true)
    private Set<String> users;

    @NotEmpty
    @ApiModelProperty(value = ModelDescriptions.GROUP, required = true)
    private String group;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Set<String> getUsers() {
        return users;
    }

    public void setUsers(Set<String> users) {
        this.users = users;
    }
}
