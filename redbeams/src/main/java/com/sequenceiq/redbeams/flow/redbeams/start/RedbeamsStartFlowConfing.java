package com.sequenceiq.redbeams.flow.redbeams.start;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent.REDBEAMS_START_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent.REDBEAMS_START_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent.REDBEAMS_START_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent.REDBEAMS_START_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent.START_DATABASE_SERVER_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent.START_DATABASE_SERVER_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartState.INIT_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartState.REDBEAMS_START_FAILED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartState.REDBEAMS_START_FINISHED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartState.START_DATABASE_SERVER_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartState.FINAL_STATE;

@Component
public class RedbeamsStartFlowConfing extends AbstractFlowConfiguration<RedbeamsStartState, RedbeamsStartEvent>
        implements RetryableFlowConfiguration<RedbeamsStartEvent> {

    private static final RedbeamsStartEvent[] REDBEAMS_INIT_EVENTS = {REDBEAMS_START_EVENT};

    private static final List<Transition<RedbeamsStartState, RedbeamsStartEvent>> TRANSITIONS =
            new Transition.Builder<RedbeamsStartState, RedbeamsStartEvent>().defaultFailureEvent(REDBEAMS_START_FAILED_EVENT)
                    .from(INIT_STATE).to(START_DATABASE_SERVER_STATE).event(REDBEAMS_START_EVENT)
                    .defaultFailureEvent()
                    .from(START_DATABASE_SERVER_STATE).to(REDBEAMS_START_FINISHED_STATE).event(START_DATABASE_SERVER_FINISHED_EVENT)
                    .failureEvent(START_DATABASE_SERVER_FAILED_EVENT)
                    .from(REDBEAMS_START_FINISHED_STATE).to(FINAL_STATE).event(REDBEAMS_START_FINISHED_EVENT).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<RedbeamsStartState, RedbeamsStartEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(RedbeamsStartState.INIT_STATE, RedbeamsStartState.FINAL_STATE, REDBEAMS_START_FAILED_STATE,
                    REDBEAMS_START_FAILURE_HANDLED_EVENT);

    public RedbeamsStartFlowConfing() {
        super(RedbeamsStartState.class, RedbeamsStartEvent.class);
    }

    @Override
    protected List<Transition<RedbeamsStartState, RedbeamsStartEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<RedbeamsStartState, RedbeamsStartEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public RedbeamsStartEvent[] getEvents() {
        return RedbeamsStartEvent.values();
    }

    @Override
    public RedbeamsStartEvent[] getInitEvents() {
        return REDBEAMS_INIT_EVENTS;
    }

    @Override
    public String getDisplayName() {
        return "Start RDS";
    }

    @Override
    public RedbeamsStartEvent getFailHandledEvent() {
        return RedbeamsStartEvent.REDBEAMS_START_FAILURE_HANDLED_EVENT;
    }
}
