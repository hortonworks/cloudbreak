package com.sequenceiq.redbeams.flow.redbeams.stop;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopEvent.REDBEAMS_STOP_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopEvent.REDBEAMS_STOP_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopEvent.REDBEAMS_STOP_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopEvent.REDBEAMS_STOP_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopEvent.STOP_DATABASE_SERVER_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopEvent.STOP_DATABASE_SERVER_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopState.INIT_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopState.REDBEAMS_STOP_FAILED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopState.REDBEAMS_STOP_FINISHED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopState.STOP_DATABASE_SERVER_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopState.FINAL_STATE;

@Component
public class RedbeamsStopFlowConfing extends AbstractFlowConfiguration<RedbeamsStopState, RedbeamsStopEvent>
    implements RetryableFlowConfiguration<RedbeamsStopEvent> {

    private static final RedbeamsStopEvent[] REDBEAMS_INIT_EVENTS = {REDBEAMS_STOP_EVENT};

    private static final List<Transition<RedbeamsStopState, RedbeamsStopEvent>> TRANSITIONS =
            new Transition.Builder<RedbeamsStopState, RedbeamsStopEvent>().defaultFailureEvent(REDBEAMS_STOP_FAILED_EVENT)
                    .from(INIT_STATE).to(STOP_DATABASE_SERVER_STATE).event(REDBEAMS_STOP_EVENT)
                        .defaultFailureEvent()
                    .from(STOP_DATABASE_SERVER_STATE).to(REDBEAMS_STOP_FINISHED_STATE).event(STOP_DATABASE_SERVER_FINISHED_EVENT)
                        .failureEvent(STOP_DATABASE_SERVER_FAILED_EVENT)
                    .from(REDBEAMS_STOP_FINISHED_STATE).to(FINAL_STATE).event(REDBEAMS_STOP_FINISHED_EVENT).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<RedbeamsStopState, RedbeamsStopEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(RedbeamsStopState.INIT_STATE, RedbeamsStopState.FINAL_STATE, REDBEAMS_STOP_FAILED_STATE, REDBEAMS_STOP_FAILURE_HANDLED_EVENT);

    public RedbeamsStopFlowConfing() {
        super(RedbeamsStopState.class, RedbeamsStopEvent.class);
    }

    @Override
    protected List<Transition<RedbeamsStopState, RedbeamsStopEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<RedbeamsStopState, RedbeamsStopEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public RedbeamsStopEvent[] getEvents() {
        return RedbeamsStopEvent.values();
    }

    @Override
    public RedbeamsStopEvent[] getInitEvents() {
        return REDBEAMS_INIT_EVENTS;
    }

    @Override
    public String getDisplayName() {
        return "Stop RDS";
    }

    @Override
    public RedbeamsStopEvent getFailHandledEvent() {
        return RedbeamsStopEvent.REDBEAMS_STOP_FAILURE_HANDLED_EVENT;
    }
}
