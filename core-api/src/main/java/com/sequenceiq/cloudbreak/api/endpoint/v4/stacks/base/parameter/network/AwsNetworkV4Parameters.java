package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.INTERNET_GATEWAY_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.LOAD_BALANCER;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.VPC_ID;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.AwsLoadBalancerParams;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsNetworkV4Parameters extends MappableBase implements JsonEntity {

    @Schema
    private String vpcId;

    @Schema
    private String internetGatewayId;

    @Schema
    private String subnetId;

    @Schema
    private String endpointGatewaySubnetId;

    @Schema
    private AwsLoadBalancerParams loadBalancer;

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

    public AwsLoadBalancerParams getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(AwsLoadBalancerParams loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, VPC_ID, vpcId);
        putIfValueNotNull(map, INTERNET_GATEWAY_ID, internetGatewayId);
        putIfValueNotNull(map, SUBNET_ID, subnetId);
        putIfValueNotNull(map, ENDPOINT_GATEWAY_SUBNET_ID, endpointGatewaySubnetId);
        putIfValueNotNull(map, LOAD_BALANCER, loadBalancer);
        return map;
    }

    @Override
    @JsonIgnore
    @Schema(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        vpcId = getParameterOrNull(parameters, VPC_ID);
        internetGatewayId = getParameterOrNull(parameters, INTERNET_GATEWAY_ID);
        subnetId = getParameterOrNull(parameters, SUBNET_ID);
        endpointGatewaySubnetId = getParameterOrNull(parameters, ENDPOINT_GATEWAY_SUBNET_ID);
        loadBalancer = getObject(parameters, LOAD_BALANCER, AwsLoadBalancerParams.class);
    }
}
