package com.sequenceiq.cloudbreak.api.model;


import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("AmbariAddress")
public class AmbariAddressJson implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.AMBARI_SERVER, required = true)
    private String ambariAddress;

    public String getAmbariAddress() {
        return ambariAddress;
    }

    public void setAmbariAddress(String ambariAddress) {
        this.ambariAddress = ambariAddress;
    }
}
