package com.sequenceiq.datalake.flow.diagnostics;

import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsEvent.SDX_DIAGNOSTICS_COLLECTION_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsEvent.SDX_DIAGNOSTICS_COLLECTION_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsEvent.SDX_DIAGNOSTICS_COLLECTION_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsEvent.SDX_DIAGNOSTICS_COLLECTION_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsEvent.SDX_DIAGNOSTICS_COLLECTION_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsEvent.SDX_DIAGNOSTICS_COLLECTION_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsState.DIAGNOSTICS_COLLECTION_FAILED_STATE;
import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsState.DIAGNOSTICS_COLLECTION_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsState.DIAGNOSTICS_COLLECTION_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsState.DIAGNOSTICS_COLLECTION_START_STATE;
import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class SdxDiagnosticsFlowConfig extends AbstractFlowConfiguration<SdxDiagnosticsState, SdxDiagnosticsEvent>
        implements RetryableDatalakeFlowConfiguration<SdxDiagnosticsEvent> {

    private static final List<Transition<SdxDiagnosticsState, SdxDiagnosticsEvent>> TRANSITIONS =
            new Transition.Builder<SdxDiagnosticsState, SdxDiagnosticsEvent>()
                    .defaultFailureEvent(SDX_DIAGNOSTICS_COLLECTION_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(DIAGNOSTICS_COLLECTION_START_STATE)
                    .event(SDX_DIAGNOSTICS_COLLECTION_EVENT).defaultFailureEvent()

                    .from(DIAGNOSTICS_COLLECTION_START_STATE)
                    .to(DIAGNOSTICS_COLLECTION_IN_PROGRESS_STATE)
                    .event(SDX_DIAGNOSTICS_COLLECTION_IN_PROGRESS_EVENT).defaultFailureEvent()

                    .from(DIAGNOSTICS_COLLECTION_IN_PROGRESS_STATE)
                    .to(DIAGNOSTICS_COLLECTION_FINISHED_STATE)
                    .event(SDX_DIAGNOSTICS_COLLECTION_SUCCESS_EVENT).defaultFailureEvent()

                    .from(DIAGNOSTICS_COLLECTION_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(SDX_DIAGNOSTICS_COLLECTION_FINALIZED_EVENT).defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<SdxDiagnosticsState, SdxDiagnosticsEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DIAGNOSTICS_COLLECTION_FAILED_STATE, SDX_DIAGNOSTICS_COLLECTION_FAILED_HANDLED_EVENT);

    protected SdxDiagnosticsFlowConfig() {
        super(SdxDiagnosticsState.class, SdxDiagnosticsEvent.class);
    }

    @Override
    protected List<Transition<SdxDiagnosticsState, SdxDiagnosticsEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<SdxDiagnosticsState, SdxDiagnosticsEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SdxDiagnosticsEvent[] getEvents() {
        return SdxDiagnosticsEvent.values();
    }

    @Override
    public SdxDiagnosticsEvent[] getInitEvents() {
        return new SdxDiagnosticsEvent[]{
            SDX_DIAGNOSTICS_COLLECTION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Collect diagnostics for SDX cluster";
    }

    @Override
    public SdxDiagnosticsEvent getRetryableEvent() {
        return SDX_DIAGNOSTICS_COLLECTION_FAILED_HANDLED_EVENT;
    }

    @Override
    public OperationType getFlowOperationType() {
        return OperationType.DIAGNOSTICS;
    }
}
