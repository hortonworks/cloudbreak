package com.sequenceiq.freeipa.flow.freeipa.diagnostics.config;

import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsCollectionsState.DIAGNOSTICS_CLEANUP_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsCollectionsState.DIAGNOSTICS_COLLECTION_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsCollectionsState.DIAGNOSTICS_COLLECTION_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsCollectionsState.DIAGNOSTICS_COLLECTION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsCollectionsState.DIAGNOSTICS_ENSURE_MACHINE_USER_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsCollectionsState.DIAGNOSTICS_INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsCollectionsState.DIAGNOSTICS_PREFLIGHT_CHECK_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsCollectionsState.DIAGNOSTICS_SALT_PILLAR_UPDATE_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsCollectionsState.DIAGNOSTICS_SALT_STATE_UPDATE_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsCollectionsState.DIAGNOSTICS_SALT_VALIDATION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsCollectionsState.DIAGNOSTICS_UPGRADE_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsCollectionsState.DIAGNOSTICS_UPLOAD_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsCollectionsState.DIAGNOSTICS_VM_PREFLIGHT_CHECK_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsCollectionsState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsCollectionsState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.FAILED_DIAGNOSTICS_COLLECTION_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.FINALIZE_DIAGNOSTICS_COLLECTION_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.FINISH_DIAGNOSTICS_COLLECTION_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.HANDLED_FAILED_DIAGNOSTICS_COLLECTION_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_CLEANUP_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_COLLECTION_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_ENSURE_MACHINE_USER_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_INIT_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_PREFLIGHT_CHECK_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_SALT_PILLAR_UPDATE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_SALT_STATE_UPDATE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_SALT_VALIDATION_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_UPGRADE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_UPLOAD_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_VM_PREFLIGHT_CHECK_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsCollectionsState;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors;

@Component
public class DiagnosticsCollectionFlowConfig extends StackStatusFinalizerAbstractFlowConfig<DiagnosticsCollectionsState, DiagnosticsCollectionStateSelectors>
        implements RetryableFlowConfiguration<DiagnosticsCollectionStateSelectors> {

    private static final List<Transition<DiagnosticsCollectionsState, DiagnosticsCollectionStateSelectors>> TRANSITIONS
            = new Transition.Builder<DiagnosticsCollectionsState, DiagnosticsCollectionStateSelectors>()
            .defaultFailureEvent(FAILED_DIAGNOSTICS_COLLECTION_EVENT)

            .from(INIT_STATE).to(DIAGNOSTICS_SALT_VALIDATION_STATE)
            .event(START_DIAGNOSTICS_SALT_VALIDATION_EVENT)
            .failureState(DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .from(DIAGNOSTICS_SALT_VALIDATION_STATE).to(DIAGNOSTICS_SALT_PILLAR_UPDATE_STATE)
            .event(START_DIAGNOSTICS_SALT_PILLAR_UPDATE_EVENT)
            .failureState(DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .from(DIAGNOSTICS_SALT_PILLAR_UPDATE_STATE).to(DIAGNOSTICS_SALT_STATE_UPDATE_STATE)
            .event(START_DIAGNOSTICS_SALT_STATE_UPDATE_EVENT)
            .failureState(DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .from(DIAGNOSTICS_SALT_STATE_UPDATE_STATE).to(DIAGNOSTICS_PREFLIGHT_CHECK_STATE)
            .event(START_DIAGNOSTICS_PREFLIGHT_CHECK_EVENT)
            .failureState(DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .from(DIAGNOSTICS_PREFLIGHT_CHECK_STATE).to(DIAGNOSTICS_INIT_STATE)
            .event(START_DIAGNOSTICS_INIT_EVENT)
            .failureState(DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .from(DIAGNOSTICS_INIT_STATE).to(DIAGNOSTICS_UPGRADE_STATE)
            .event(START_DIAGNOSTICS_UPGRADE_EVENT)
            .failureState(DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .from(DIAGNOSTICS_UPGRADE_STATE).to(DIAGNOSTICS_VM_PREFLIGHT_CHECK_STATE)
            .event(START_DIAGNOSTICS_VM_PREFLIGHT_CHECK_EVENT)
            .failureState(DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .from(DIAGNOSTICS_VM_PREFLIGHT_CHECK_STATE).to(DIAGNOSTICS_ENSURE_MACHINE_USER_STATE)
            .event(START_DIAGNOSTICS_ENSURE_MACHINE_USER_EVENT)
            .failureState(DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .from(DIAGNOSTICS_ENSURE_MACHINE_USER_STATE).to(DIAGNOSTICS_COLLECTION_STATE)
            .event(START_DIAGNOSTICS_COLLECTION_EVENT)
            .failureState(DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .from(DIAGNOSTICS_COLLECTION_STATE).to(DIAGNOSTICS_UPLOAD_STATE)
            .event(START_DIAGNOSTICS_UPLOAD_EVENT)
            .failureState(DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .from(DIAGNOSTICS_UPLOAD_STATE).to(DIAGNOSTICS_CLEANUP_STATE)
            .event(START_DIAGNOSTICS_CLEANUP_EVENT)
            .failureState(DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .from(DIAGNOSTICS_CLEANUP_STATE).to(DIAGNOSTICS_COLLECTION_FINISHED_STATE)
            .event(FINISH_DIAGNOSTICS_COLLECTION_EVENT)
            .failureState(DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .from(DIAGNOSTICS_COLLECTION_FINISHED_STATE).to(FINAL_STATE)
            .event(FINALIZE_DIAGNOSTICS_COLLECTION_EVENT)
            .failureState(DIAGNOSTICS_COLLECTION_FAILED_STATE)
            .defaultFailureEvent()

            .build();

    private static final FlowEdgeConfig<DiagnosticsCollectionsState, DiagnosticsCollectionStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DIAGNOSTICS_COLLECTION_FAILED_STATE, HANDLED_FAILED_DIAGNOSTICS_COLLECTION_EVENT);

    protected DiagnosticsCollectionFlowConfig() {
        super(DiagnosticsCollectionsState.class, DiagnosticsCollectionStateSelectors.class);
    }

    @Override
    protected List<Transition<DiagnosticsCollectionsState, DiagnosticsCollectionStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DiagnosticsCollectionsState, DiagnosticsCollectionStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DiagnosticsCollectionStateSelectors[] getEvents() {
        return DiagnosticsCollectionStateSelectors.values();
    }

    @Override
    public DiagnosticsCollectionStateSelectors[] getInitEvents() {
        return new DiagnosticsCollectionStateSelectors[] {
                START_DIAGNOSTICS_SALT_VALIDATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Diagnostics collection flow";
    }

    @Override
    public DiagnosticsCollectionStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_DIAGNOSTICS_COLLECTION_EVENT;
    }

    @Override
    public OperationType getFlowOperationType() {
        return OperationType.DIAGNOSTICS;
    }
}
