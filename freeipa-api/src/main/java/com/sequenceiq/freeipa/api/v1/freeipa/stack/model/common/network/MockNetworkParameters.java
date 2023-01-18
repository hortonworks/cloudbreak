package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MockNetworkV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class MockNetworkParameters extends MappableBase {

    @Schema
    private String vpcId;

    @Schema
    private String internetGatewayId;

    @Schema
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
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, "vpcId", vpcId);
        putIfValueNotNull(map, "internetGatewayId", internetGatewayId);
        putIfValueNotNull(map, SUBNET_ID, subnetId);
        return map;
    }

    @Override
    @JsonIgnore
    @Schema(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.MOCK;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        vpcId = getParameterOrNull(parameters, "vpcId");
        internetGatewayId = getParameterOrNull(parameters, "internetGatewayId");
        subnetId = getParameterOrNull(parameters, SUBNET_ID);
    }

    @Override
    public String toString() {
        return "MockNetworkParameters{"
                + "vpcId='" + vpcId + '\''
                + ", internetGatewayId='" + internetGatewayId + '\''
                + ", subnetId='" + subnetId + '\''
                + '}';
    }
}
