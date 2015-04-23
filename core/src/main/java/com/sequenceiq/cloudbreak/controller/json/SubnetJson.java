package com.sequenceiq.cloudbreak.controller.json;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel("Subnet")
public class SubnetJson implements JsonEntity {

    @ApiModelProperty(required = true)
    private String subnet;

    public SubnetJson() {
    }

    public SubnetJson(String subnet) {
        this.subnet = subnet;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }
}
