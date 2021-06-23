package com.sequenceiq.environment.environment.dto;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;

public class EnvironmentLoadBalancerDto implements Payload {

    private Long id;

    private EnvironmentDto environmentDto;

    private PublicEndpointAccessGateway endpointAccessGateway;

    private Set<String> endpointGatewaySubnetIds;

    private String flowId;

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getResourceId() {
        return id;
    }

    public EnvironmentDto getEnvironmentDto() {
        return environmentDto;
    }

    public void setEnvironmentDto(EnvironmentDto environmentDto) {
        this.environmentDto = environmentDto;
    }

    public void setEndpointAccessGateway(PublicEndpointAccessGateway endpointAccessGateway) {
        this.endpointAccessGateway = endpointAccessGateway;
    }

    public PublicEndpointAccessGateway getEndpointAccessGateway() {
        return endpointAccessGateway;
    }

    public Set<String> getEndpointGatewaySubnetIds() {
        return endpointGatewaySubnetIds;
    }

    public void setEndpointGatewaySubnetIds(Set<String> endpointGatewaySubnetIds) {
        this.endpointGatewaySubnetIds = endpointGatewaySubnetIds;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    @Override
    public String toString() {
        return "EnvironmentLoadBalancerDto{" +
            "id=" + id +
            ", endpointAccessGateway=" + endpointAccessGateway +
            ", endpointGatewaySubnetIds=" + endpointGatewaySubnetIds +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Long id;

        private EnvironmentDto environmentDto;

        private PublicEndpointAccessGateway endpointAccessGateway;

        private Set<String> endpointGatewaySubnetIds;

        private String flowId;

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withEnvironmentDto(EnvironmentDto environmentDto) {
            this.environmentDto = environmentDto;
            return this;
        }

        public Builder withEndpointAccessGateway(PublicEndpointAccessGateway endpointAccessGateway) {
            this.endpointAccessGateway = endpointAccessGateway;
            return this;
        }

        public Builder withEndpointGatewaySubnetIds(Set<String> endpointGatewaySubnetIds) {
            this.endpointGatewaySubnetIds = endpointGatewaySubnetIds;
            return this;
        }

        public Builder withFlowId(String flowId) {
            this.flowId = flowId;
            return this;
        }

        public EnvironmentLoadBalancerDto build() {
            EnvironmentLoadBalancerDto environmentLoadBalancerDto = new EnvironmentLoadBalancerDto();
            environmentLoadBalancerDto.setId(id);
            environmentLoadBalancerDto.setEnvironmentDto(environmentDto);
            environmentLoadBalancerDto.setEndpointAccessGateway(endpointAccessGateway);
            environmentLoadBalancerDto.setEndpointGatewaySubnetIds(endpointGatewaySubnetIds);
            environmentLoadBalancerDto.setFlowId(flowId);
            return environmentLoadBalancerDto;
        }
    }
}
