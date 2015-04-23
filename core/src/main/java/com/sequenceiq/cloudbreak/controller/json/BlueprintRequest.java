package com.sequenceiq.cloudbreak.controller.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.BlueprintModelDescription;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlueprintRequest extends BlueprintBase {
    @ApiModelProperty(value = BlueprintModelDescription.URL)
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
