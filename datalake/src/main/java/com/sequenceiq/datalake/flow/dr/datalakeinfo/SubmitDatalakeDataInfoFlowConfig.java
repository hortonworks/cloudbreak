package com.sequenceiq.datalake.flow.dr.datalakeinfo;

import static com.sequenceiq.datalake.flow.dr.datalakeinfo.SubmitDatalakeDataInfoEvent.SUBMIT_DATALAKE_DATA_INFO_EVENT;
import static com.sequenceiq.datalake.flow.dr.datalakeinfo.SubmitDatalakeDataInfoEvent.SUBMIT_DATALAKE_DATA_INFO_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.dr.datalakeinfo.SubmitDatalakeDataInfoEvent.SUBMIT_DATALAKE_DATA_INFO_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.datalakeinfo.SubmitDatalakeDataInfoEvent.SUBMIT_DATALAKE_DATA_INFO_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.datalakeinfo.SubmitDatalakeDataInfoState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.dr.datalakeinfo.SubmitDatalakeDataInfoState.INIT_STATE;
import static com.sequenceiq.datalake.flow.dr.datalakeinfo.SubmitDatalakeDataInfoState.SUBMIT_DATALAKE_DATA_INFO_FAILED_STATE;
import static com.sequenceiq.datalake.flow.dr.datalakeinfo.SubmitDatalakeDataInfoState.SUBMIT_DATALAKE_DATA_INFO_IN_PROGRESS_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class SubmitDatalakeDataInfoFlowConfig extends AbstractFlowConfiguration<SubmitDatalakeDataInfoState, SubmitDatalakeDataInfoEvent>
        implements RetryableFlowConfiguration<SubmitDatalakeDataInfoEvent> {
    private static final List<Transition<SubmitDatalakeDataInfoState, SubmitDatalakeDataInfoEvent>> TRANSITIONS =
            new Transition.Builder<SubmitDatalakeDataInfoState, SubmitDatalakeDataInfoEvent>()
                    .defaultFailureEvent(SUBMIT_DATALAKE_DATA_INFO_FAILED_EVENT)

                    .from(INIT_STATE).to(SUBMIT_DATALAKE_DATA_INFO_IN_PROGRESS_STATE)
                    .event(SUBMIT_DATALAKE_DATA_INFO_EVENT).noFailureEvent()

                    .from(SUBMIT_DATALAKE_DATA_INFO_IN_PROGRESS_STATE).to(FINAL_STATE)
                    .event(SUBMIT_DATALAKE_DATA_INFO_SUCCESS_EVENT).defaultFailureEvent()

                    .from(SUBMIT_DATALAKE_DATA_INFO_FAILED_STATE).to(FINAL_STATE)
                    .event(SUBMIT_DATALAKE_DATA_INFO_FAILURE_HANDLED_EVENT).noFailureEvent()

                    .build();

    private static final FlowEdgeConfig<SubmitDatalakeDataInfoState, SubmitDatalakeDataInfoEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SUBMIT_DATALAKE_DATA_INFO_FAILED_STATE, SUBMIT_DATALAKE_DATA_INFO_FAILURE_HANDLED_EVENT);

    public SubmitDatalakeDataInfoFlowConfig() {
        super(SubmitDatalakeDataInfoState.class, SubmitDatalakeDataInfoEvent.class);
    }

    @Override
    public SubmitDatalakeDataInfoEvent[] getEvents() {
        return SubmitDatalakeDataInfoEvent.values();
    }

    @Override
    public SubmitDatalakeDataInfoEvent[] getInitEvents() {
        return new SubmitDatalakeDataInfoEvent[] {
                SUBMIT_DATALAKE_DATA_INFO_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Submit datalake data info";
    }

    @Override
    protected List<Transition<SubmitDatalakeDataInfoState, SubmitDatalakeDataInfoEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<SubmitDatalakeDataInfoState, SubmitDatalakeDataInfoEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SubmitDatalakeDataInfoEvent getRetryableEvent() {
        return SUBMIT_DATALAKE_DATA_INFO_FAILURE_HANDLED_EVENT;
    }
}
