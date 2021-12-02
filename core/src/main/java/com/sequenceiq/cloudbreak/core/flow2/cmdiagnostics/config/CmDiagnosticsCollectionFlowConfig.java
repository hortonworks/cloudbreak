package com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.config;

import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.CmDiagnosticsCollectionState.CM_DIAGNOSTICS_CLEANUP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.CmDiagnosticsCollectionState.CM_DIAGNOSTICS_COLLECTION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.CmDiagnosticsCollectionState.CM_DIAGNOSTICS_COLLECTION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.CmDiagnosticsCollectionState.CM_DIAGNOSTICS_COLLECTION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.CmDiagnosticsCollectionState.CM_DIAGNOSTICS_INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.CmDiagnosticsCollectionState.CM_DIAGNOSTICS_UPLOAD_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.CmDiagnosticsCollectionState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.CmDiagnosticsCollectionState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event.CmDiagnosticsCollectionStateSelectors.FAILED_CM_DIAGNOSTICS_COLLECTION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event.CmDiagnosticsCollectionStateSelectors.FINALIZE_CM_DIAGNOSTICS_COLLECTION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event.CmDiagnosticsCollectionStateSelectors.FINISH_CM_DIAGNOSTICS_COLLECTION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event.CmDiagnosticsCollectionStateSelectors.HANDLED_FAILED_CM_DIAGNOSTICS_COLLECTION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event.CmDiagnosticsCollectionStateSelectors.START_CM_DIAGNOSTICS_CLEANUP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event.CmDiagnosticsCollectionStateSelectors.START_CM_DIAGNOSTICS_COLLECTION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event.CmDiagnosticsCollectionStateSelectors.START_CM_DIAGNOSTICS_INIT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event.CmDiagnosticsCollectionStateSelectors.START_CM_DIAGNOSTICS_UPLOAD_EVENT;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.CmDiagnosticsCollectionState;
import com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event.CmDiagnosticsCollectionStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class CmDiagnosticsCollectionFlowConfig extends AbstractFlowConfiguration<CmDiagnosticsCollectionState, CmDiagnosticsCollectionStateSelectors>
        implements RetryableFlowConfiguration<CmDiagnosticsCollectionStateSelectors> {

    private static final List<Transition<CmDiagnosticsCollectionState, CmDiagnosticsCollectionStateSelectors>> TRANSITIONS
            = new Transition.Builder<CmDiagnosticsCollectionState, CmDiagnosticsCollectionStateSelectors>()
            .defaultFailureEvent(FAILED_CM_DIAGNOSTICS_COLLECTION_EVENT)

            .from(INIT_STATE).to(CM_DIAGNOSTICS_INIT_STATE)
            .event(START_CM_DIAGNOSTICS_INIT_EVENT)
            .failureState(CM_DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .from(CM_DIAGNOSTICS_INIT_STATE).to(CM_DIAGNOSTICS_COLLECTION_STATE)
            .event(START_CM_DIAGNOSTICS_COLLECTION_EVENT)
            .failureState(CM_DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .from(CM_DIAGNOSTICS_COLLECTION_STATE).to(CM_DIAGNOSTICS_UPLOAD_STATE)
            .event(START_CM_DIAGNOSTICS_UPLOAD_EVENT)
            .failureState(CM_DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .from(CM_DIAGNOSTICS_UPLOAD_STATE).to(CM_DIAGNOSTICS_CLEANUP_STATE)
            .event(START_CM_DIAGNOSTICS_CLEANUP_EVENT)
            .failureState(CM_DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .from(CM_DIAGNOSTICS_CLEANUP_STATE).to(CM_DIAGNOSTICS_COLLECTION_FINISHED_STATE)
            .event(FINISH_CM_DIAGNOSTICS_COLLECTION_EVENT)
            .failureState(CM_DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .from(CM_DIAGNOSTICS_COLLECTION_FINISHED_STATE).to(FINAL_STATE)
            .event(FINALIZE_CM_DIAGNOSTICS_COLLECTION_EVENT)
            .failureState(CM_DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .build();

    private static final FlowEdgeConfig<CmDiagnosticsCollectionState, CmDiagnosticsCollectionStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, CM_DIAGNOSTICS_COLLECTION_FAILED_STATE, HANDLED_FAILED_CM_DIAGNOSTICS_COLLECTION_EVENT);

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

    protected CmDiagnosticsCollectionFlowConfig() {
        super(CmDiagnosticsCollectionState.class, CmDiagnosticsCollectionStateSelectors.class);
    }

    @Override
    protected List<Transition<CmDiagnosticsCollectionState, CmDiagnosticsCollectionStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<CmDiagnosticsCollectionState, CmDiagnosticsCollectionStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public CmDiagnosticsCollectionStateSelectors[] getEvents() {
        return CmDiagnosticsCollectionStateSelectors.values();
    }

    @Override
    public CmDiagnosticsCollectionStateSelectors[] getInitEvents() {
        return new CmDiagnosticsCollectionStateSelectors[] {
                START_CM_DIAGNOSTICS_INIT_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "CM diagnostics collection flow";
    }

    @Override
    public CmDiagnosticsCollectionStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_CM_DIAGNOSTICS_COLLECTION_EVENT;
    }

    @Override
    public FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}
