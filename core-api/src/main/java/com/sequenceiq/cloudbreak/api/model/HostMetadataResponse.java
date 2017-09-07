package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostMetadataModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("HostMetadata")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HostMetadataResponse extends HostMetadataBase {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(HostMetadataModelDescription.STATE)
    private String state;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
