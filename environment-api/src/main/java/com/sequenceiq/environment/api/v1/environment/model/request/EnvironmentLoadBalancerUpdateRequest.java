package com.sequenceiq.environment.api.v1.environment.model.request;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentLoadBalancerUpdateRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentLoadBalancerUpdateRequest {

    @Schema(description = EnvironmentModelDescription.PUBLIC_ENDPOINT_ACCESS_GATEWAY)
    private PublicEndpointAccessGateway publicEndpointAccessGateway = PublicEndpointAccessGateway.DISABLED;

    @Schema(description = EnvironmentModelDescription.ENDPOINT_ACCESS_GATEWAY_SUBNET_IDS)
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
