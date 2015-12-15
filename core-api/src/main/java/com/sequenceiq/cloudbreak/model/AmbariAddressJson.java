package com.sequenceiq.cloudbreak.model;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

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
