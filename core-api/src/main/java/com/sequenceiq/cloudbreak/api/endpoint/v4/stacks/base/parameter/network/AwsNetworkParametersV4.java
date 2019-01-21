package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class AwsNetworkParametersV4 implements JsonEntity, Mappable {

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
    public <T> T toClass(Map<String, Object> parameters) {
        AwsNetworkParametersV4 ret = new AwsNetworkParametersV4();
        ret.vpcId = getParameterOrNull(parameters, "vpcId");
        ret.internetGatewayId = getParameterOrNull(parameters,"internetGatewayId");
        ret.subnetId = getParameterOrNull(parameters,"subnetId");
        return (T) ret;
    }
}
