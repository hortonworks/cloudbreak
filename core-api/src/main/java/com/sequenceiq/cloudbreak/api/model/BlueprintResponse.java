package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("BlueprintResponse")
public class BlueprintResponse extends BlueprintBase {

    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(BlueprintModelDescription.BLUEPRINT_NAME)
    private String blueprintName;

    @ApiModelProperty(BlueprintModelDescription.HOST_GROUP_COUNT)
    private Integer hostGroupCount;

    @ApiModelProperty(ModelDescriptions.PUBLIC_IN_ACCOUNT)
    private boolean publicInAccount;

    @ApiModelProperty(BlueprintModelDescription.STATUS)
    private ResourceStatus status;

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

}
