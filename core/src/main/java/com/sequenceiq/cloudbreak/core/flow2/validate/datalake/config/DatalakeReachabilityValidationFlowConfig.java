package com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config;

import static com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config.DatalakeReachabilityValidationEvent.DATALAKE_REACHABILITY_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config.DatalakeReachabilityValidationEvent.DATALAKE_REACHABILITY_VALIDATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config.DatalakeReachabilityValidationEvent.DATALAKE_REACHABILITY_VALIDATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config.DatalakeReachabilityValidationEvent.DATALAKE_REACHABILITY_VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config.DatalakeReachabilityValidationState.DATALAKE_REACHABILITY_VALIDATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config.DatalakeReachabilityValidationState.DATALAKE_REACHABILITY_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config.DatalakeReachabilityValidationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config.DatalakeReachabilityValidationState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class DatalakeReachabilityValidationFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<DatalakeReachabilityValidationState, DatalakeReachabilityValidationEvent>
        implements RetryableFlowConfiguration<DatalakeReachabilityValidationEvent> {

    private static final List<Transition<DatalakeReachabilityValidationState, DatalakeReachabilityValidationEvent>> TRANSITIONS =
            new Builder<DatalakeReachabilityValidationState, DatalakeReachabilityValidationEvent>()
            .defaultFailureEvent(DATALAKE_REACHABILITY_VALIDATION_FAILED_EVENT)
            .from(INIT_STATE).to(DATALAKE_REACHABILITY_VALIDATION_STATE).event(DATALAKE_REACHABILITY_VALIDATION_EVENT).defaultFailureEvent()
            .from(DATALAKE_REACHABILITY_VALIDATION_STATE).to(FINAL_STATE).event(DATALAKE_REACHABILITY_VALIDATION_FINISHED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<DatalakeReachabilityValidationState, DatalakeReachabilityValidationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATALAKE_REACHABILITY_VALIDATION_FAILED_STATE, DATALAKE_REACHABILITY_VALIDATION_FAILURE_HANDLED_EVENT);

    public DatalakeReachabilityValidationFlowConfig() {
        super(DatalakeReachabilityValidationState.class, DatalakeReachabilityValidationEvent.class);
    }

    @Override
    protected List<Transition<DatalakeReachabilityValidationState, DatalakeReachabilityValidationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatalakeReachabilityValidationState, DatalakeReachabilityValidationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DatalakeReachabilityValidationEvent[] getEvents() {
        return DatalakeReachabilityValidationEvent.values();
    }

    @Override
    public DatalakeReachabilityValidationEvent[] getInitEvents() {
        return new DatalakeReachabilityValidationEvent[] {
                DATALAKE_REACHABILITY_VALIDATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Data Lake reachability validation for stack";
    }

    @Override
    public DatalakeReachabilityValidationEvent getRetryableEvent() {
        return DATALAKE_REACHABILITY_VALIDATION_FAILURE_HANDLED_EVENT;
    }
}
