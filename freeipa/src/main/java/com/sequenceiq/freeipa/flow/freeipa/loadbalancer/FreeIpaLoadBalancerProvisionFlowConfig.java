package com.sequenceiq.freeipa.flow.freeipa.loadbalancer;

import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerCreationEvent.CONFIGURATION_CREATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerCreationEvent.FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerCreationEvent.FREEIPA_LOAD_BALANCER_CREATION_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerCreationEvent.FREEIPA_LOAD_BALANCER_CREATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerCreationEvent.FREEIPA_LOAD_BALANCER_CREATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerCreationEvent.METADATA_COLLECTION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerCreationEvent.PROVISION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerProvisionState.CREATE_CONFIGURATION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerProvisionState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerProvisionState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerProvisionState.LOAD_BALANCER_CREATION_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerProvisionState.METADATA_COLLECTION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerProvisionState.PROVISIONING_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerProvisionState.PROVISION_FAILED_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class FreeIpaLoadBalancerProvisionFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<FreeIpaLoadBalancerProvisionState, FreeIpaLoadBalancerCreationEvent>
        implements RetryableFlowConfiguration<FreeIpaLoadBalancerCreationEvent> {

    private static final FreeIpaLoadBalancerCreationEvent[] INIT_EVENTS = { FREEIPA_LOAD_BALANCER_CREATION_EVENT };

    private static final FlowEdgeConfig<FreeIpaLoadBalancerProvisionState, FreeIpaLoadBalancerCreationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, PROVISION_FAILED_STATE, FREEIPA_LOAD_BALANCER_CREATION_FAILURE_HANDLED_EVENT);

    private static final List<Transition<FreeIpaLoadBalancerProvisionState, FreeIpaLoadBalancerCreationEvent>> TRANSITIONS
            = new Builder<FreeIpaLoadBalancerProvisionState, FreeIpaLoadBalancerCreationEvent>().defaultFailureEvent(FAILURE_EVENT)

            .from(INIT_STATE)
            .to(CREATE_CONFIGURATION_STATE)
            .event(FREEIPA_LOAD_BALANCER_CREATION_EVENT)
            .noFailureEvent()

            .from(CREATE_CONFIGURATION_STATE)
            .to(PROVISIONING_STATE)
            .event(CONFIGURATION_CREATION_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(PROVISIONING_STATE)
            .to(METADATA_COLLECTION_STATE)
            .event(PROVISION_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(METADATA_COLLECTION_STATE)
            .to(LOAD_BALANCER_CREATION_FINISHED_STATE)
            .event(METADATA_COLLECTION_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(LOAD_BALANCER_CREATION_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(FREEIPA_LOAD_BALANCER_CREATION_FINISHED_EVENT)
            .defaultFailureEvent()

            .build();

    public FreeIpaLoadBalancerProvisionFlowConfig() {
        super(FreeIpaLoadBalancerProvisionState.class, FreeIpaLoadBalancerCreationEvent.class);
    }

    @Override
    protected List<Transition<FreeIpaLoadBalancerProvisionState, FreeIpaLoadBalancerCreationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<FreeIpaLoadBalancerProvisionState, FreeIpaLoadBalancerCreationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public FreeIpaLoadBalancerCreationEvent[] getEvents() {
        return FreeIpaLoadBalancerCreationEvent.values();
    }

    @Override
    public FreeIpaLoadBalancerCreationEvent[] getInitEvents() {
        return INIT_EVENTS;
    }

    @Override
    public String getDisplayName() {
        return "Provision FreeIPA load balancer";
    }

    @Override
    public FreeIpaLoadBalancerCreationEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }
}
