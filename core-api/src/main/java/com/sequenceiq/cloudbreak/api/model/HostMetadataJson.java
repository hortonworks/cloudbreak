package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("HostMetadata")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HostMetadataJson {

    @ApiModelProperty(value = ModelDescriptions.ID)
    private Long id;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.HostGroupModelDescription.HOST_GROUP_NAME, required = true)
    private String groupName;

    @ApiModelProperty(value = ModelDescriptions.HostMetadataModelDescription.STATE)
    private String state;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
