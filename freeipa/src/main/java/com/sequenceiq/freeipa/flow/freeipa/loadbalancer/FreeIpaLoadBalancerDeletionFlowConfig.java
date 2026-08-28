package com.sequenceiq.freeipa.flow.freeipa.loadbalancer;

import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerDeletionEvent.LOAD_BALANCER_CLOUD_DELETION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerDeletionEvent.LOAD_BALANCER_DELETION_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerDeletionEvent.LOAD_BALANCER_DELETION_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerDeletionEvent.LOAD_BALANCER_DELETION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerDeletionEvent.LOAD_BALANCER_DELETION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerDeletionEvent.LOAD_BALANCER_DNS_DEREGISTRATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerDeletionState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerDeletionState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerDeletionState.LOAD_BALANCER_CLOUD_DELETION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerDeletionState.LOAD_BALANCER_DELETION_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerDeletionState.LOAD_BALANCER_DELETION_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerDeletionState.LOAD_BALANCER_DNS_DEREGISTRATION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class FreeIpaLoadBalancerDeletionFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<FreeIpaLoadBalancerDeletionState, FreeIpaLoadBalancerDeletionEvent>
        implements RetryableFlowConfiguration<FreeIpaLoadBalancerDeletionEvent> {

    private static final FreeIpaLoadBalancerDeletionEvent[] INIT_EVENTS = { LOAD_BALANCER_DELETION_EVENT };

    private static final FlowEdgeConfig<FreeIpaLoadBalancerDeletionState, FreeIpaLoadBalancerDeletionEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, LOAD_BALANCER_DELETION_FAILED_STATE, LOAD_BALANCER_DELETION_FAILURE_HANDLED_EVENT);

    private static final List<Transition<FreeIpaLoadBalancerDeletionState, FreeIpaLoadBalancerDeletionEvent>> TRANSITIONS
            = new Builder<FreeIpaLoadBalancerDeletionState, FreeIpaLoadBalancerDeletionEvent>().defaultFailureEvent(LOAD_BALANCER_DELETION_FAILURE_EVENT)

            .from(INIT_STATE)
            .to(LOAD_BALANCER_DNS_DEREGISTRATION_STATE)
            .event(LOAD_BALANCER_DELETION_EVENT)
            .noFailureEvent()

            .from(LOAD_BALANCER_DNS_DEREGISTRATION_STATE)
            .to(LOAD_BALANCER_CLOUD_DELETION_STATE)
            .event(LOAD_BALANCER_DNS_DEREGISTRATION_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(LOAD_BALANCER_CLOUD_DELETION_STATE)
            .to(LOAD_BALANCER_DELETION_FINISHED_STATE)
            .event(LOAD_BALANCER_CLOUD_DELETION_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(LOAD_BALANCER_DELETION_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(LOAD_BALANCER_DELETION_FINISHED_EVENT)
            .defaultFailureEvent()

            .build();

    public FreeIpaLoadBalancerDeletionFlowConfig() {
        super(FreeIpaLoadBalancerDeletionState.class, FreeIpaLoadBalancerDeletionEvent.class);
    }

    @Override
    protected List<Transition<FreeIpaLoadBalancerDeletionState, FreeIpaLoadBalancerDeletionEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<FreeIpaLoadBalancerDeletionState, FreeIpaLoadBalancerDeletionEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public FreeIpaLoadBalancerDeletionEvent[] getEvents() {
        return FreeIpaLoadBalancerDeletionEvent.values();
    }

    @Override
    public FreeIpaLoadBalancerDeletionEvent[] getInitEvents() {
        return INIT_EVENTS;
    }

    @Override
    public String getDisplayName() {
        return "Delete FreeIPA load balancer";
    }

    @Override
    public FreeIpaLoadBalancerDeletionEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }
}
