package com.sequenceiq.redbeams.flow.redbeams.termination;

import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent.DEREGISTER_DATABASE_SERVER_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent.DEREGISTER_DATABASE_SERVER_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent.REDBEAMS_TERMINATION_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent.REDBEAMS_TERMINATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent.REDBEAMS_TERMINATION_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent.TERMINATE_DATABASE_SERVER_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent.TERMINATE_DATABASE_SERVER_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationState.DEREGISTER_DATABASE_SERVER_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationState.FINAL_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationState.INIT_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationState.REDBEAMS_TERMINATION_FAILED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationState.REDBEAMS_TERMINATION_FINISHED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationState.TERMINATE_DATABASE_SERVER_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class RedbeamsTerminationFlowConfig extends AbstractFlowConfiguration<RedbeamsTerminationState, RedbeamsTerminationEvent>
        implements RetryableFlowConfiguration<RedbeamsTerminationEvent> {

    private static final RedbeamsTerminationEvent[] REDBEAMS_INIT_EVENTS = {REDBEAMS_TERMINATION_EVENT};

    private static final List<Transition<RedbeamsTerminationState, RedbeamsTerminationEvent>> TRANSITIONS =
            new Builder<RedbeamsTerminationState, RedbeamsTerminationEvent>().defaultFailureEvent(REDBEAMS_TERMINATION_FAILED_EVENT)
            .from(INIT_STATE).to(TERMINATE_DATABASE_SERVER_STATE).event(REDBEAMS_TERMINATION_EVENT).defaultFailureEvent()
            .from(TERMINATE_DATABASE_SERVER_STATE).to(DEREGISTER_DATABASE_SERVER_STATE).event(TERMINATE_DATABASE_SERVER_FINISHED_EVENT)
                    .failureEvent(TERMINATE_DATABASE_SERVER_FAILED_EVENT)
            .from(DEREGISTER_DATABASE_SERVER_STATE).to(REDBEAMS_TERMINATION_FINISHED_STATE).event(DEREGISTER_DATABASE_SERVER_FINISHED_EVENT)
                    .failureEvent(DEREGISTER_DATABASE_SERVER_FAILED_EVENT)
            .from(REDBEAMS_TERMINATION_FINISHED_STATE).to(FINAL_STATE).event(REDBEAMS_TERMINATION_FINISHED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<RedbeamsTerminationState, RedbeamsTerminationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, REDBEAMS_TERMINATION_FAILED_STATE, REDBEAMS_TERMINATION_FAILURE_HANDLED_EVENT);

    public RedbeamsTerminationFlowConfig() {
        super(RedbeamsTerminationState.class, RedbeamsTerminationEvent.class);
    }

    @Override
    protected List<Transition<RedbeamsTerminationState, RedbeamsTerminationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<RedbeamsTerminationState, RedbeamsTerminationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public RedbeamsTerminationEvent[] getEvents() {
        return RedbeamsTerminationEvent.values();
    }

    @Override
    public RedbeamsTerminationEvent[] getInitEvents() {
        return REDBEAMS_INIT_EVENTS;
    }

    @Override
    public String getDisplayName() {
        return "Terminate RDS";
    }

    @Override
    public RedbeamsTerminationEvent getFailHandledEvent() {
        return REDBEAMS_TERMINATION_FAILURE_HANDLED_EVENT;
    }
}
