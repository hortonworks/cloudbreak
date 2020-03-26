package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request;


import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class AmbariAddressV4Request implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.AMBARI_SERVER, required = true)
    private String ambariAddress;

    public String getAmbariAddress() {
        return ambariAddress;
    }

    public void setAmbariAddress(String ambariAddress) {
        this.ambariAddress = ambariAddress;
    }
}
