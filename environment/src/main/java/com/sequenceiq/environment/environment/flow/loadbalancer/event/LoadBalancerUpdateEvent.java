package com.sequenceiq.environment.environment.flow.loadbalancer.event;

import java.util.Set;

import reactor.rx.Promise;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class LoadBalancerUpdateEvent extends BaseNamedFlowEvent {

    private final Environment environment;

    private final EnvironmentDto environmentDto;

    private final PublicEndpointAccessGateway endpointAccessGateway;

    private final Set<String> subnetIds;

    private String flowId;

    public LoadBalancerUpdateEvent(String selector, Long resourceId, String resourceName, String resourceCrn,
            Environment environment, EnvironmentDto environmentDto, PublicEndpointAccessGateway endpointAccessGateway,
            Set<String> subnetIds, String flowId) {
        super(selector, resourceId, resourceName, resourceCrn);
        this.environment = environment;
        this.environmentDto = environmentDto;
        this.endpointAccessGateway = endpointAccessGateway;
        this.subnetIds = subnetIds;
        this.flowId = flowId;
    }

    public LoadBalancerUpdateEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, String resourceName,
            String resourceCrn, Environment environment, EnvironmentDto environmentDto, PublicEndpointAccessGateway endpointAccessGateway,
            Set<String> subnetIds, String flowId) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.environment = environment;
        this.environmentDto = environmentDto;
        this.endpointAccessGateway = endpointAccessGateway;
        this.subnetIds = subnetIds;
        this.flowId = flowId;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public EnvironmentDto getEnvironmentDto() {
        return environmentDto;
    }

    public PublicEndpointAccessGateway getEndpointAccessGateway() {
        return endpointAccessGateway;
    }

    public Set<String> getSubnetIds() {
        return subnetIds;
    }

    public String getFlowId() {
        return flowId;
    }

    public static final class LoadBalancerUpdateEventBuilder {

        private Environment environment;

        private EnvironmentDto environmentDto;

        private String resourceName;

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private PublicEndpointAccessGateway endpointAccessGateway;

        private Set<String> subnetIds;

        private String flowId;

        private LoadBalancerUpdateEventBuilder() {
        }

        public static LoadBalancerUpdateEventBuilder aLoadBalancerUpdateEvent() {
            return new LoadBalancerUpdateEventBuilder();
        }

        public LoadBalancerUpdateEventBuilder withEnvironment(Environment environment) {
            this.environment = environment;
            return this;
        }

        public LoadBalancerUpdateEventBuilder withEnvironmentDto(EnvironmentDto environmentDto) {
            this.environmentDto = environmentDto;
            return this;
        }

        public LoadBalancerUpdateEventBuilder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public LoadBalancerUpdateEventBuilder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public LoadBalancerUpdateEventBuilder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public LoadBalancerUpdateEventBuilder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public LoadBalancerUpdateEventBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public LoadBalancerUpdateEventBuilder withPublicEndpointAccessGateway(PublicEndpointAccessGateway endpointAccessGateway) {
            this.endpointAccessGateway = endpointAccessGateway;
            return this;
        }

        public LoadBalancerUpdateEventBuilder withSubnetIds(Set<String> subnetIds) {
            this.subnetIds = subnetIds;
            return this;
        }

        public LoadBalancerUpdateEventBuilder withFlowId(String flowId) {
            this.flowId = flowId;
            return this;
        }

        public LoadBalancerUpdateEvent build() {
            LoadBalancerUpdateEvent event = new LoadBalancerUpdateEvent(selector, resourceId, accepted,
                resourceName, resourceCrn, environment, environmentDto, endpointAccessGateway, subnetIds, flowId);
            return event;
        }
    }
}
