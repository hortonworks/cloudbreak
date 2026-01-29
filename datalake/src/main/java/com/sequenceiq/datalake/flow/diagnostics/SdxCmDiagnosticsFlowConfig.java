package com.sequenceiq.datalake.flow.diagnostics;

import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsEvent.SDX_CM_DIAGNOSTICS_COLLECTION_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsEvent.SDX_CM_DIAGNOSTICS_COLLECTION_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsEvent.SDX_CM_DIAGNOSTICS_COLLECTION_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsEvent.SDX_CM_DIAGNOSTICS_COLLECTION_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsEvent.SDX_CM_DIAGNOSTICS_COLLECTION_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsEvent.SDX_CM_DIAGNOSTICS_COLLECTION_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsState.CM_DIAGNOSTICS_COLLECTION_FAILED_STATE;
import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsState.CM_DIAGNOSTICS_COLLECTION_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsState.CM_DIAGNOSTICS_COLLECTION_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsState.CM_DIAGNOSTICS_COLLECTION_START_STATE;
import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class SdxCmDiagnosticsFlowConfig extends AbstractFlowConfiguration<SdxCmDiagnosticsState, SdxCmDiagnosticsEvent>
        implements RetryableDatalakeFlowConfiguration<SdxCmDiagnosticsEvent> {

    private static final List<Transition<SdxCmDiagnosticsState, SdxCmDiagnosticsEvent>> TRANSITIONS =
            new Transition.Builder<SdxCmDiagnosticsState, SdxCmDiagnosticsEvent>()
                    .defaultFailureEvent(SDX_CM_DIAGNOSTICS_COLLECTION_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(CM_DIAGNOSTICS_COLLECTION_START_STATE)
                    .event(SDX_CM_DIAGNOSTICS_COLLECTION_EVENT).defaultFailureEvent()

                    .from(CM_DIAGNOSTICS_COLLECTION_START_STATE)
                    .to(CM_DIAGNOSTICS_COLLECTION_IN_PROGRESS_STATE)
                    .event(SDX_CM_DIAGNOSTICS_COLLECTION_IN_PROGRESS_EVENT).defaultFailureEvent()

                    .from(CM_DIAGNOSTICS_COLLECTION_IN_PROGRESS_STATE)
                    .to(CM_DIAGNOSTICS_COLLECTION_FINISHED_STATE)
                    .event(SDX_CM_DIAGNOSTICS_COLLECTION_SUCCESS_EVENT).defaultFailureEvent()

                    .from(CM_DIAGNOSTICS_COLLECTION_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(SDX_CM_DIAGNOSTICS_COLLECTION_FINALIZED_EVENT).defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<SdxCmDiagnosticsState, SdxCmDiagnosticsEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, CM_DIAGNOSTICS_COLLECTION_FAILED_STATE, SDX_CM_DIAGNOSTICS_COLLECTION_FAILED_HANDLED_EVENT);

    protected SdxCmDiagnosticsFlowConfig() {
        super(SdxCmDiagnosticsState.class, SdxCmDiagnosticsEvent.class);
    }

    @Override
    protected List<Transition<SdxCmDiagnosticsState, SdxCmDiagnosticsEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<SdxCmDiagnosticsState, SdxCmDiagnosticsEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SdxCmDiagnosticsEvent[] getEvents() {
        return SdxCmDiagnosticsEvent.values();
    }

    @Override
    public SdxCmDiagnosticsEvent[] getInitEvents() {
        return new SdxCmDiagnosticsEvent[]{
                SDX_CM_DIAGNOSTICS_COLLECTION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Collect CM based diagnostics for SDX cluster";
    }

    @Override
    public SdxCmDiagnosticsEvent getRetryableEvent() {
        return SDX_CM_DIAGNOSTICS_COLLECTION_FAILED_HANDLED_EVENT;
    }
}
