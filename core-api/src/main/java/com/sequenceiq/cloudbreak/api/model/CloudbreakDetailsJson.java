package com.sequenceiq.cloudbreak.api.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudbreakDetailsJson {

    @ApiModelProperty(ModelDescriptions.CloudbreakDetailsModelDescription.VERSION)
    private String version;

    public CloudbreakDetailsJson() { }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
