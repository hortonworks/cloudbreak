package com.sequenceiq.cloudbreak.controller.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.BlueprintModelDescription;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class BlueprintResponse extends BlueprintBase {
    @ApiModelProperty(value = ModelDescriptions.ID)
    private String id;
    @ApiModelProperty(value = BlueprintModelDescription.BLUEPRINT_NAME)
    private String blueprintName;
    @ApiModelProperty(value = BlueprintModelDescription.HOST_GROUP_COUNT)
    private Integer hostGroupCount;
    @ApiModelProperty(value = ModelDescriptions.PUBLIC_IN_ACCOUNT)
    private boolean publicInAccount;

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
}
