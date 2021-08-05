package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "InstanceGroupAwsNetworkV1Parameters")
public class InstanceGroupAwsNetworkParameters implements Serializable {

    @ApiModelProperty
    private List<String> subnetIds = new ArrayList<>();

    public List<String> getSubnetIds() {
        return subnetIds;
    }

    public void setSubnetIds(List<String> subnetIds) {
        this.subnetIds = subnetIds;
    }

    @Override
    public String toString() {
        return "InstanceGroupAwsNetworkParameters{" +
                "subnetIds=" + subnetIds +
                '}';
    }
}
