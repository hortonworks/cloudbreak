package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config;

import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationEvent.EXTERNAL_DATABASE_TERMINATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationEvent.EXTERNAL_DATABASE_TERMINATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationEvent.EXTERNAL_DATABASE_TERMINATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationEvent.EXTERNAL_DATABASE_WAIT_TERMINATION_SUCCESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationEvent.START_EXTERNAL_DATABASE_TERMINATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationState.EXTERNAL_DATABASE_TERMINATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationState.EXTERNAL_DATABASE_TERMINATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationState.WAIT_FOR_EXTERNAL_DATABASE_TERMINATION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ExternalDatabaseTerminationFlowConfig extends AbstractFlowConfiguration<ExternalDatabaseTerminationState, ExternalDatabaseTerminationEvent>
        implements RetryableFlowConfiguration<ExternalDatabaseTerminationEvent> {

    private static final List<Transition<ExternalDatabaseTerminationState, ExternalDatabaseTerminationEvent>> TRANSITIONS =
            new Builder<ExternalDatabaseTerminationState, ExternalDatabaseTerminationEvent>()
            .defaultFailureEvent(EXTERNAL_DATABASE_TERMINATION_FAILED_EVENT)
            .from(INIT_STATE)
            .to(WAIT_FOR_EXTERNAL_DATABASE_TERMINATION_STATE)
            .event(START_EXTERNAL_DATABASE_TERMINATION_EVENT).defaultFailureEvent()
            .from(WAIT_FOR_EXTERNAL_DATABASE_TERMINATION_STATE)
            .to(EXTERNAL_DATABASE_TERMINATION_FINISHED_STATE)
            .event(EXTERNAL_DATABASE_WAIT_TERMINATION_SUCCESS_EVENT).defaultFailureEvent()
            .from(EXTERNAL_DATABASE_TERMINATION_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(EXTERNAL_DATABASE_TERMINATION_FINISHED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<ExternalDatabaseTerminationState, ExternalDatabaseTerminationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, EXTERNAL_DATABASE_TERMINATION_FAILED_STATE, EXTERNAL_DATABASE_TERMINATION_FAILURE_HANDLED_EVENT);

    public ExternalDatabaseTerminationFlowConfig() {
        super(ExternalDatabaseTerminationState.class, ExternalDatabaseTerminationEvent.class);
    }

    @Override
    protected List<Transition<ExternalDatabaseTerminationState, ExternalDatabaseTerminationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ExternalDatabaseTerminationState, ExternalDatabaseTerminationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ExternalDatabaseTerminationEvent[] getEvents() {
        return ExternalDatabaseTerminationEvent.values();
    }

    @Override
    public ExternalDatabaseTerminationEvent[] getInitEvents() {
        return new ExternalDatabaseTerminationEvent[] {
                START_EXTERNAL_DATABASE_TERMINATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Terminate external database of the stack";
    }

    @Override
    public ExternalDatabaseTerminationEvent getRetryableEvent() {
        return EXTERNAL_DATABASE_TERMINATION_FAILURE_HANDLED_EVENT;
    }
}
