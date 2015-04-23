package com.sequenceiq.cloudbreak.controller.json;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel("AmbariAddress")
public class AmbariAddressJson implements JsonEntity {

    @ApiModelProperty(required = true)
    private String ambariAddress;

    public String getAmbariAddress() {
        return ambariAddress;
    }

    public void setAmbariAddress(String ambariAddress) {
        this.ambariAddress = ambariAddress;
    }
}
