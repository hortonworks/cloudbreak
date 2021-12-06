package com.sequenceiq.environment.environment.flow.loadbalancer.event;

import java.util.Objects;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

import reactor.rx.Promise;

public class LoadBalancerUpdateEvent extends BaseNamedFlowEvent {

    private final Environment environment;

    private final EnvironmentDto environmentDto;

    private final PublicEndpointAccessGateway endpointAccessGateway;

    private final Set<String> subnetIds;

    public LoadBalancerUpdateEvent(String selector, Long resourceId, String resourceName, String resourceCrn,
            Environment environment, EnvironmentDto environmentDto, PublicEndpointAccessGateway endpointAccessGateway, Set<String> subnetIds) {
        super(selector, resourceId, resourceName, resourceCrn);
        this.environment = environment;
        this.environmentDto = environmentDto;
        this.endpointAccessGateway = endpointAccessGateway;
        this.subnetIds = subnetIds;
    }

    public LoadBalancerUpdateEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, String resourceName, String resourceCrn,
            Environment environment, EnvironmentDto environmentDto, PublicEndpointAccessGateway endpointAccessGateway, Set<String> subnetIds) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.environment = environment;
        this.environmentDto = environmentDto;
        this.endpointAccessGateway = endpointAccessGateway;
        this.subnetIds = subnetIds;
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

    @Override
    public boolean equalsEvent(BaseFlowEvent other) {
        return isClassAndEqualsEvent(LoadBalancerUpdateEvent.class, other,
                event -> Objects.equals(subnetIds, event.subnetIds)
                        && Objects.equals(endpointAccessGateway, event.endpointAccessGateway));
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

        public LoadBalancerUpdateEvent build() {
            LoadBalancerUpdateEvent event = new LoadBalancerUpdateEvent(selector, resourceId, accepted,
                resourceName, resourceCrn, environment, environmentDto, endpointAccessGateway, subnetIds);
            return event;
        }
    }
}
