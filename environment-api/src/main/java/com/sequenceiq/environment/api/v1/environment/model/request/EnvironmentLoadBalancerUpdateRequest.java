package com.sequenceiq.environment.api.v1.environment.model.request;

import java.util.Set;

import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "EnvironmentLoadBalancerUpdateRequest")
public class EnvironmentLoadBalancerUpdateRequest {

    @ApiModelProperty(EnvironmentModelDescription.PUBLIC_ENDPOINT_ACCESS_GATEWAY)
    private PublicEndpointAccessGateway publicEndpointAccessGateway = PublicEndpointAccessGateway.DISABLED;

    @ApiModelProperty(EnvironmentModelDescription.ENDPOINT_ACCESS_GATEWAY_SUBNET_IDS)
    private Set<String> subnetIds = Set.of();

    public PublicEndpointAccessGateway getPublicEndpointAccessGateway() {
        return publicEndpointAccessGateway;
    }

    public void setPublicEndpointAccessGateway(PublicEndpointAccessGateway publicEndpointAccessGateway) {
        this.publicEndpointAccessGateway = publicEndpointAccessGateway;
    }

    public Set<String> getSubnetIds() {
        return subnetIds;
    }

    public void setSubnetIds(Set<String> subnetIds) {
        this.subnetIds = subnetIds;
    }

    @Override
    public String toString() {
        return "EnvironmentEndpointGatewayRequest{" +
            "publicEndpointAccessGateway=" + publicEndpointAccessGateway +
            ", subnetIds=" + subnetIds +
            '}';
    }
}
