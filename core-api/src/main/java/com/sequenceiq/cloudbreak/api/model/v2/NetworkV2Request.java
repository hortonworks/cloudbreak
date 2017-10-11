package com.sequenceiq.cloudbreak.api.model.v2;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.validation.ValidSubnet;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkV2Request implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.NetworkModelDescription.SUBNET_CIDR)
    @ValidSubnet
    private String subnetCIDR;

    @ApiModelProperty(ModelDescriptions.NetworkModelDescription.PARAMETERS)
    private Map<String, Object> parameters = new HashMap<>();

    public String getSubnetCIDR() {
        return subnetCIDR;
    }

    public void setSubnetCIDR(String subnetCIDR) {
        this.subnetCIDR = subnetCIDR;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}
