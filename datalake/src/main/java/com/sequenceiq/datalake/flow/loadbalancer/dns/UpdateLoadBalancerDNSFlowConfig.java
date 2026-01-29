package com.sequenceiq.datalake.flow.loadbalancer.dns;

import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSEvent.UPDATE_LOAD_BALANCER_DNS_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSEvent.UPDATE_LOAD_BALANCER_DNS_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSEvent.UPDATE_LOAD_BALANCER_DNS_IPA_EVENT;
import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSEvent.UPDATE_LOAD_BALANCER_DNS_IPA_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSEvent.UPDATE_LOAD_BALANCER_DNS_PEM_EVENT;
import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSEvent.UPDATE_LOAD_BALANCER_DNS_PEM_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSState.INIT_STATE;
import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSState.UPDATE_LOAD_BALANCER_DNS_FAILED_STATE;
import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSState.UPDATE_LOAD_BALANCER_DNS_IPA_STATE;
import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSState.UPDATE_LOAD_BALANCER_DNS_PEM_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class UpdateLoadBalancerDNSFlowConfig extends AbstractFlowConfiguration<UpdateLoadBalancerDNSState, UpdateLoadBalancerDNSEvent>
        implements RetryableDatalakeFlowConfiguration<UpdateLoadBalancerDNSEvent> {
    private static final List<Transition<UpdateLoadBalancerDNSState, UpdateLoadBalancerDNSEvent>> TRANSITIONS =
        new Builder<UpdateLoadBalancerDNSState, UpdateLoadBalancerDNSEvent>()
        .defaultFailureEvent(UPDATE_LOAD_BALANCER_DNS_FAILED_EVENT)

        .from(INIT_STATE)
        .to(UPDATE_LOAD_BALANCER_DNS_PEM_STATE)
        .event(UPDATE_LOAD_BALANCER_DNS_PEM_EVENT).noFailureEvent()

        .from(INIT_STATE)
        .to(UPDATE_LOAD_BALANCER_DNS_IPA_STATE)
        .event(UPDATE_LOAD_BALANCER_DNS_IPA_EVENT).noFailureEvent()

        .from(UPDATE_LOAD_BALANCER_DNS_PEM_STATE)
        .to(UPDATE_LOAD_BALANCER_DNS_IPA_STATE)
        .event(UPDATE_LOAD_BALANCER_DNS_PEM_SUCCESS_EVENT).defaultFailureEvent()

        .from(UPDATE_LOAD_BALANCER_DNS_IPA_STATE)
        .to(FINAL_STATE)
        .event(UPDATE_LOAD_BALANCER_DNS_IPA_SUCCESS_EVENT).defaultFailureEvent()

        .from(UPDATE_LOAD_BALANCER_DNS_FAILED_STATE)
        .to(FINAL_STATE)
        .event(UPDATE_LOAD_BALANCER_DNS_FAILURE_HANDLED_EVENT).noFailureEvent()

        .build();

    private static final FlowEdgeConfig<UpdateLoadBalancerDNSState, UpdateLoadBalancerDNSEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, UPDATE_LOAD_BALANCER_DNS_FAILED_STATE, UPDATE_LOAD_BALANCER_DNS_FAILURE_HANDLED_EVENT);

    public UpdateLoadBalancerDNSFlowConfig() {
        super(UpdateLoadBalancerDNSState.class, UpdateLoadBalancerDNSEvent.class);
    }

    @Override
    public UpdateLoadBalancerDNSEvent[] getEvents() {
        return UpdateLoadBalancerDNSEvent.values();
    }

    @Override
    public UpdateLoadBalancerDNSEvent[] getInitEvents() {
        return new UpdateLoadBalancerDNSEvent[] {
                UPDATE_LOAD_BALANCER_DNS_PEM_EVENT,
                UPDATE_LOAD_BALANCER_DNS_IPA_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Update load balancer DNS";
    }

    @Override
    protected List<Transition<UpdateLoadBalancerDNSState, UpdateLoadBalancerDNSEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<UpdateLoadBalancerDNSState, UpdateLoadBalancerDNSEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public UpdateLoadBalancerDNSEvent getRetryableEvent() {
        return UPDATE_LOAD_BALANCER_DNS_FAILURE_HANDLED_EVENT;
    }
}
