package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.Mappable;

import io.swagger.annotations.ApiModelProperty;

public class MockNetworkV4Parameters implements JsonEntity, Mappable {

    @ApiModelProperty
    private String vpcId;

    @ApiModelProperty
    private String internetGatewayId;

    @ApiModelProperty
    private String subnetId;

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getInternetGatewayId() {
        return internetGatewayId;
    }

    public void setInternetGatewayId(String internetGatewayId) {
        this.internetGatewayId = internetGatewayId;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("vpcId", vpcId);
        map.put("internetGatewayId", internetGatewayId);
        map.put("subnetId", subnetId);
        return map;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        vpcId = getParameterOrNull(parameters, "vpcId");
        internetGatewayId = getParameterOrNull(parameters, "internetGatewayId");
        subnetId = getParameterOrNull(parameters, "subnetId");
    }
}
