package com.sequenceiq.freeipa.flow.freeipa.loadbalancer;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerCreationFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.configuration.LoadBalancerConfigurationSuccess;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.metadata.LoadBalancerMetadataCollectionSuccess;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.provision.LoadBalancerProvisionSuccess;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerDomainUpdateSuccess;

public enum FreeIpaLoadBalancerCreationEvent implements FlowEvent {
    FREEIPA_LOAD_BALANCER_CREATION_EVENT,
    CONFIGURATION_CREATION_FINISHED_EVENT(EventSelectorUtil.selector(LoadBalancerConfigurationSuccess.class)),
    PROVISION_FINISHED_EVENT(EventSelectorUtil.selector(LoadBalancerProvisionSuccess.class)),
    METADATA_COLLECTION_FINISHED_EVENT(EventSelectorUtil.selector(LoadBalancerMetadataCollectionSuccess.class)),
    LOAD_BALANCER_DOMAIN_UPDATE_FINISHED_EVENT(EventSelectorUtil.selector(LoadBalancerDomainUpdateSuccess.class)),
    FREEIPA_LOAD_BALANCER_CREATION_FINISHED_EVENT,
    FAILURE_EVENT(EventSelectorUtil.selector(LoadBalancerCreationFailureEvent.class)),
    FREEIPA_LOAD_BALANCER_CREATION_FAILURE_HANDLED_EVENT;

    private final String event;

    FreeIpaLoadBalancerCreationEvent(String event) {
        this.event = event;
    }

    FreeIpaLoadBalancerCreationEvent() {
        this.event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
