package com.sequenceiq.freeipa.api.v1.freeipa.test.model;

import java.util.Set;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("CheckGroupsV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckGroupsV1Request extends ClientTestBaseRequest {

    @NotEmpty
    @ApiModelProperty(value = ModelDescriptions.GROUPS, required = true)
    private Set<String> groups;

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }
}
