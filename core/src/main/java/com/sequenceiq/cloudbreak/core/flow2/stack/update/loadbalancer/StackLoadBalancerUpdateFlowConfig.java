package com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer;

import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.COLLECT_LOAD_BALANCER_METADATA_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.COLLECT_LOAD_BALANCER_METADATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.CREATE_CLOUD_LOAD_BALANCERS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.CREATE_CLOUD_LOAD_BALANCERS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.CREATE_LOAD_BALANCER_ENTITY_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.CREATE_LOAD_BALANCER_ENTITY_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.LOAD_BALANCER_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.RESTART_CM_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.RESTART_CM_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.STACK_LOAD_BALANCER_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.LOAD_BALANCER_UPDATE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.LOAD_BALANCER_UPDATE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.REGISTER_FREEIPA_DNS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.REGISTER_FREEIPA_DNS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.REGISTER_PUBLIC_DNS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.REGISTER_PUBLIC_DNS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.UPDATE_SERVICE_CONFIG_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.UPDATE_SERVICE_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateState.COLLECTING_LOAD_BALANCER_METADATA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateState.CREATING_CLOUD_LOAD_BALANCERS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateState.CREATING_LOAD_BALANCER_ENTITY_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateState.LOAD_BALANCER_UPDATE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateState.LOAD_BALANCER_UPDATE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateState.REGISTERING_FREEIPA_DNS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateState.REGISTERING_PUBLIC_DNS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateState.RESTARTING_CM_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateState.UPDATING_SERVICE_CONFIG_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class StackLoadBalancerUpdateFlowConfig extends AbstractFlowConfiguration<StackLoadBalancerUpdateState, StackLoadBalancerUpdateEvent>
    implements RetryableFlowConfiguration<StackLoadBalancerUpdateEvent> {

    private static final List<Transition<StackLoadBalancerUpdateState, StackLoadBalancerUpdateEvent>> TRANSITIONS =
        new Builder<StackLoadBalancerUpdateState, StackLoadBalancerUpdateEvent>().defaultFailureEvent(LOAD_BALANCER_UPDATE_FAILED)

        .from(INIT_STATE)
            .to(CREATING_LOAD_BALANCER_ENTITY_STATE)
            .event(STACK_LOAD_BALANCER_UPDATE_EVENT)
            .noFailureEvent()

        .from(CREATING_LOAD_BALANCER_ENTITY_STATE)
            .to(CREATING_CLOUD_LOAD_BALANCERS_STATE)
            .event(CREATE_LOAD_BALANCER_ENTITY_FINISHED_EVENT)
            .failureEvent(CREATE_LOAD_BALANCER_ENTITY_FAILED_EVENT)

        .from(CREATING_CLOUD_LOAD_BALANCERS_STATE)
            .to(COLLECTING_LOAD_BALANCER_METADATA_STATE)
            .event(CREATE_CLOUD_LOAD_BALANCERS_FINISHED_EVENT)
            .failureEvent(CREATE_CLOUD_LOAD_BALANCERS_FAILED_EVENT)

        .from(COLLECTING_LOAD_BALANCER_METADATA_STATE)
            .to(REGISTERING_PUBLIC_DNS_STATE)
            .event(COLLECT_LOAD_BALANCER_METADATA_FINISHED_EVENT)
            .failureEvent(COLLECT_LOAD_BALANCER_METADATA_FAILED_EVENT)

        .from(REGISTERING_PUBLIC_DNS_STATE)
            .to(REGISTERING_FREEIPA_DNS_STATE)
            .event(REGISTER_PUBLIC_DNS_FINISHED_EVENT)
            .failureEvent(REGISTER_PUBLIC_DNS_FAILED_EVENT)

        .from(REGISTERING_FREEIPA_DNS_STATE)
            .to(UPDATING_SERVICE_CONFIG_STATE)
            .event(REGISTER_FREEIPA_DNS_FINISHED_EVENT)
            .failureEvent(REGISTER_FREEIPA_DNS_FAILED_EVENT)

        .from(UPDATING_SERVICE_CONFIG_STATE)
            .to(RESTARTING_CM_STATE)
            .event(UPDATE_SERVICE_CONFIG_FINISHED_EVENT)
            .failureEvent(UPDATE_SERVICE_CONFIG_FAILED_EVENT)

        .from(RESTARTING_CM_STATE)
            .to(LOAD_BALANCER_UPDATE_FINISHED_STATE)
            .event(RESTART_CM_FINISHED_EVENT)
            .failureEvent(RESTART_CM_FAILED_EVENT)

        .from(LOAD_BALANCER_UPDATE_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(LOAD_BALANCER_UPDATE_FINISHED_EVENT).defaultFailureEvent()

        .build();

    private static final FlowEdgeConfig<StackLoadBalancerUpdateState, StackLoadBalancerUpdateEvent> EDGE_CONFIG =
        new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, LOAD_BALANCER_UPDATE_FAILED_STATE, LOAD_BALANCER_UPDATE_FAIL_HANDLED_EVENT);

    public StackLoadBalancerUpdateFlowConfig() {
        super(StackLoadBalancerUpdateState.class, StackLoadBalancerUpdateEvent.class);
    }

    @Override
    public StackLoadBalancerUpdateEvent[] getEvents() {
        return StackLoadBalancerUpdateEvent.values();
    }

    @Override
    public StackLoadBalancerUpdateEvent[] getInitEvents() {
        return new StackLoadBalancerUpdateEvent[]{
            STACK_LOAD_BALANCER_UPDATE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Create load balancers";
    }

    @Override
    protected List<Transition<StackLoadBalancerUpdateState, StackLoadBalancerUpdateEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<StackLoadBalancerUpdateState, StackLoadBalancerUpdateEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public StackLoadBalancerUpdateEvent getRetryableEvent() {
        return LOAD_BALANCER_UPDATE_FAIL_HANDLED_EVENT;
    }

    @Override
    public FlowTriggerCondition getFlowTriggerCondition() {
        return getApplicationContext().getBean(StackLoadBalancerUpdateTriggerCondition.class);
    }
}
