package com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.WorkspaceV4Base;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class WorkspaceV4Response extends WorkspaceV4Base {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    private Set<UserV4Response> users;

    private WorkspaceStatus status;

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<UserV4Response> getUsers() {
        return users;
    }

    public void setUsers(Set<UserV4Response> users) {
        this.users = users;
    }

    public WorkspaceStatus getStatus() {
        return status;
    }

    public void setStatus(WorkspaceStatus status) {
        this.status = status;
    }

}
