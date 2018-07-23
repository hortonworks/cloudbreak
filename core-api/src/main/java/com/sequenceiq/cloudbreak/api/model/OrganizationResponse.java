package com.sequenceiq.cloudbreak.api.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class OrganizationResponse extends OrganizationBase {
    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    private Set<UserOrgPermissionsJson> users;

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<UserOrgPermissionsJson> getUsers() {
        return users;
    }

    public void setUsers(Set<UserOrgPermissionsJson> users) {
        this.users = users;
    }
}
