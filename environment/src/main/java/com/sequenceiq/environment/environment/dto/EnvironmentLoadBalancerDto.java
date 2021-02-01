package com.sequenceiq.environment.environment.dto;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.environment.domain.Environment;

public class EnvironmentLoadBalancerDto implements Payload {

    private Long id;

    private Environment environment;

    private EnvironmentDto environmentDto;

    private PublicEndpointAccessGateway endpointAccessGateway;

    private Set<String> endpointGatewaySubnetIds;

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getResourceId() {
        return id;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
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

        private Environment environment;

        private EnvironmentDto environmentDto;

        private PublicEndpointAccessGateway endpointAccessGateway;

        private Set<String> endpointGatewaySubnetIds;

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withEnvironment(Environment environment) {
            this.environment = environment;
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

        public EnvironmentLoadBalancerDto build() {
            EnvironmentLoadBalancerDto environmentLoadBalancerDto = new EnvironmentLoadBalancerDto();
            environmentLoadBalancerDto.setId(id);
            environmentLoadBalancerDto.setEnvironment(environment);
            environmentLoadBalancerDto.setEnvironmentDto(environmentDto);
            environmentLoadBalancerDto.setEndpointAccessGateway(endpointAccessGateway);
            environmentLoadBalancerDto.setEndpointGatewaySubnetIds(endpointGatewaySubnetIds);
            return environmentLoadBalancerDto;
        }
    }
}
