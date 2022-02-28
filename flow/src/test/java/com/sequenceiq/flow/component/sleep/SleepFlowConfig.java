package com.sequenceiq.flow.component.sleep;

import java.util.List;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.flow.component.sleep.event.SleepEvent;

public class SleepFlowConfig extends AbstractFlowConfiguration<SleepState, SleepEvent>
        implements RetryableFlowConfiguration<SleepEvent> {

    protected SleepFlowConfig() {
        super(SleepState.class, SleepEvent.class);
    }

    @Override
    protected List<Transition<SleepState, SleepEvent>> getTransitions() {
        return new Builder<SleepState, SleepEvent>()
                .defaultFailureEvent(SleepEvent.SLEEP_FAILED_EVENT)

                .from(SleepState.INIT_STATE)
                .to(SleepState.SLEEP_STARTED_STATE)
                .event(SleepEvent.SLEEP_STARTED_EVENT)
                .noFailureEvent()

                .from(SleepState.SLEEP_STARTED_STATE)
                .to(SleepState.SLEEP_FINISHED_STATE)
                .event(SleepEvent.SLEEP_COMPLETED_EVENT)
                .defaultFailureEvent()

                .from(SleepState.SLEEP_FINISHED_STATE)
                .to(SleepState.FINAL_STATE)
                .event(SleepEvent.SLEEP_FINALIZED_EVENT)
                .defaultFailureEvent()

                .build();
    }

    @Override
    protected FlowEdgeConfig<SleepState, SleepEvent> getEdgeConfig() {
        return new FlowEdgeConfig<>(
                SleepState.INIT_STATE,
                SleepState.FINAL_STATE,
                SleepState.SLEEP_FAILED_STATE,
                SleepEvent.SLEEP_FAIL_HANDLED_EVENT);
    }

    @Override
    public SleepEvent[] getEvents() {
        return SleepEvent.values();
    }

    @Override
    public SleepEvent[] getInitEvents() {
        return new SleepEvent[]{SleepEvent.SLEEP_STARTED_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Sleep";
    }

    @Override
    public SleepEvent getRetryableEvent() {
        return SleepEvent.SLEEP_FAILED_EVENT;
    }
}