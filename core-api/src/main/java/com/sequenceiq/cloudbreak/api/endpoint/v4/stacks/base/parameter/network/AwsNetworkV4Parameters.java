package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.INTERNET_GATEWAY_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.VPC_ID;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsNetworkV4Parameters extends MappableBase implements JsonEntity {

    @ApiModelProperty
    private String vpcId;

    @ApiModelProperty
    private String internetGatewayId;

    @ApiModelProperty
    private String subnetId;

    @ApiModelProperty
    private String endpointGatewaySubnetId;

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

    public String getEndpointGatewaySubnetId() {
        return endpointGatewaySubnetId;
    }

    public void setEndpointGatewaySubnetId(String endpointGatewaySubnetId) {
        this.endpointGatewaySubnetId = endpointGatewaySubnetId;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, VPC_ID, vpcId);
        putIfValueNotNull(map, INTERNET_GATEWAY_ID, internetGatewayId);
        putIfValueNotNull(map, SUBNET_ID, subnetId);
        putIfValueNotNull(map, ENDPOINT_GATEWAY_SUBNET_ID, endpointGatewaySubnetId);
        return map;
    }

    @Override
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        vpcId = getParameterOrNull(parameters, VPC_ID);
        internetGatewayId = getParameterOrNull(parameters, INTERNET_GATEWAY_ID);
        subnetId = getParameterOrNull(parameters, SUBNET_ID);
        endpointGatewaySubnetId = getParameterOrNull(parameters, ENDPOINT_GATEWAY_SUBNET_ID);
    }
}
