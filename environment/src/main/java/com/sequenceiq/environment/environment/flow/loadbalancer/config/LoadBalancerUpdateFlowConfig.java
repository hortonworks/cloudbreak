package com.sequenceiq.environment.environment.flow.loadbalancer.config;

import static com.sequenceiq.environment.environment.flow.loadbalancer.LoadBalancerUpdateState.ENVIRONMENT_UPDATE_STATE;
import static com.sequenceiq.environment.environment.flow.loadbalancer.LoadBalancerUpdateState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.loadbalancer.LoadBalancerUpdateState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.loadbalancer.LoadBalancerUpdateState.LOAD_BALANCER_UPDATE_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.loadbalancer.LoadBalancerUpdateState.LOAD_BALANCER_UPDATE_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.loadbalancer.LoadBalancerUpdateState.STACK_UPDATE_STATE;
import static com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateStateSelectors.FAILED_LOAD_BALANCER_UPDATE_EVENT;
import static com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateStateSelectors.FINALIZE_LOAD_BALANCER_UPDATE_EVENT;
import static com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateStateSelectors.FINISH_LOAD_BALANCER_UPDATE_EVENT;
import static com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateStateSelectors.HANDLED_FAILED_LOAD_BALANCER_UPDATE_EVENT;
import static com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateStateSelectors.LOAD_BALANCER_STACK_UPDATE_EVENT;
import static com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateStateSelectors.LOAD_BALANCER_UPDATE_START_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.flow.loadbalancer.LoadBalancerUpdateState;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class LoadBalancerUpdateFlowConfig extends AbstractFlowConfiguration<LoadBalancerUpdateState, LoadBalancerUpdateStateSelectors>
    implements RetryableFlowConfiguration<LoadBalancerUpdateStateSelectors> {

    private static final List<Transition<LoadBalancerUpdateState, LoadBalancerUpdateStateSelectors>> TRANSITIONS =
        new Transition.Builder<LoadBalancerUpdateState, LoadBalancerUpdateStateSelectors>()
        .defaultFailureEvent(FAILED_LOAD_BALANCER_UPDATE_EVENT)

        .from(INIT_STATE).to(ENVIRONMENT_UPDATE_STATE)
        .event(LOAD_BALANCER_UPDATE_START_EVENT).defaultFailureEvent()

        .from(ENVIRONMENT_UPDATE_STATE).to(STACK_UPDATE_STATE)
        .event(LOAD_BALANCER_STACK_UPDATE_EVENT).defaultFailureEvent()

        .from(STACK_UPDATE_STATE).to(LOAD_BALANCER_UPDATE_FINISHED_STATE)
        .event(FINISH_LOAD_BALANCER_UPDATE_EVENT).defaultFailureEvent()

        .from(LOAD_BALANCER_UPDATE_FINISHED_STATE).to(FINAL_STATE)
        .event(FINALIZE_LOAD_BALANCER_UPDATE_EVENT).defaultFailureEvent()

        .build();

    protected LoadBalancerUpdateFlowConfig() {
        super(LoadBalancerUpdateState.class, LoadBalancerUpdateStateSelectors.class);
    }

    @Override
    protected List<Transition<LoadBalancerUpdateState, LoadBalancerUpdateStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<LoadBalancerUpdateState, LoadBalancerUpdateStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, LOAD_BALANCER_UPDATE_FAILED_STATE, HANDLED_FAILED_LOAD_BALANCER_UPDATE_EVENT);
    }

    @Override
    public LoadBalancerUpdateStateSelectors[] getEvents() {
        return LoadBalancerUpdateStateSelectors.values();
    }

    @Override
    public LoadBalancerUpdateStateSelectors[] getInitEvents() {
        return new LoadBalancerUpdateStateSelectors[]{LOAD_BALANCER_UPDATE_START_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Update load balancers in environment";
    }

    @Override
    public LoadBalancerUpdateStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_LOAD_BALANCER_UPDATE_EVENT;
    }
}
