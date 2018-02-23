package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RecommendationRequestModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendationRequestJson extends PlatformResourceRequestJson {

    @ApiModelProperty(RecommendationRequestModelDescription.BLUEPRINT_NAME)
    private String blueprintName;

    @ApiModelProperty(RecommendationRequestModelDescription.BLUEPRINT_ID)
    private Long blueprintId;

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }
}
