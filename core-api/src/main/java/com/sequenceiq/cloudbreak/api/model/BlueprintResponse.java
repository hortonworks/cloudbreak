package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class BlueprintResponse extends BlueprintBase {
    @ApiModelProperty(value = ModelDescriptions.ID)
    private String id;
    @ApiModelProperty(value = BlueprintModelDescription.BLUEPRINT_NAME)
    private String blueprintName;
    @ApiModelProperty(value = BlueprintModelDescription.HOST_GROUP_COUNT)
    private Integer hostGroupCount;
    @ApiModelProperty(value = ModelDescriptions.PUBLIC_IN_ACCOUNT)
    private boolean publicInAccount;
    @ApiModelProperty(value = BlueprintModelDescription.STATUS)
    private ResourceStatus status;
    @ApiModelProperty(value = BlueprintModelDescription.INPUTS)
    private Set<BlueprintParameterJson> inputs = new HashSet<>();

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public Integer getHostGroupCount() {
        return hostGroupCount;
    }

    public void setHostGroupCount(Integer hostGroupCount) {
        this.hostGroupCount = hostGroupCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("public")
    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public Set<BlueprintParameterJson> getInputs() {
        return inputs;
    }

    public void setInputs(Set<BlueprintParameterJson> inputs) {
        this.inputs = inputs;
    }

}
