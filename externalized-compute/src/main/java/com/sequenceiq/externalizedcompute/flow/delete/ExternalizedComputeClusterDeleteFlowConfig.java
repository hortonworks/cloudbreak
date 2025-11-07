package com.sequenceiq.externalizedcompute.flow.delete;

import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_AUX_CLUSTER_DELETE_INITIATED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_AUX_CLUSTER_DELETE_STARTED;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAILED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINALIZED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINISHED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_INITIATED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_STARTED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteState.EXTERNALIZED_COMPUTE_CLUSTER_AUXILIARY_DELETE_IN_PROGRESS_STATE;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteState.EXTERNALIZED_COMPUTE_CLUSTER_AUXILIARY_DELETE_STATE;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteState.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAILED_STATE;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteState.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINISHED_STATE;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteState.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_IN_PROGRESS_STATE;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteState.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_STATE;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteState.FINAL_STATE;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ExternalizedComputeClusterDeleteFlowConfig
        extends AbstractFlowConfiguration<ExternalizedComputeClusterDeleteState, ExternalizedComputeClusterDeleteFlowEvent>
        implements RetryableFlowConfiguration<ExternalizedComputeClusterDeleteFlowEvent> {

    private static final List<Transition<ExternalizedComputeClusterDeleteState, ExternalizedComputeClusterDeleteFlowEvent>> TRANSITIONS =
            new Transition.Builder<ExternalizedComputeClusterDeleteState, ExternalizedComputeClusterDeleteFlowEvent>()
                    .defaultFailureEvent(EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAILED_EVENT)
                    .from(INIT_STATE)
                    .to(EXTERNALIZED_COMPUTE_CLUSTER_AUXILIARY_DELETE_STATE)
                    .event(EXTERNALIZED_COMPUTE_CLUSTER_DELETE_AUX_CLUSTER_DELETE_INITIATED_EVENT).defaultFailureEvent()

                    .from(INIT_STATE)
                    .to(EXTERNALIZED_COMPUTE_CLUSTER_DELETE_STATE)
                    .event(EXTERNALIZED_COMPUTE_CLUSTER_DELETE_INITIATED_EVENT).defaultFailureEvent()

                    .from(EXTERNALIZED_COMPUTE_CLUSTER_AUXILIARY_DELETE_STATE)
                    .to(EXTERNALIZED_COMPUTE_CLUSTER_AUXILIARY_DELETE_IN_PROGRESS_STATE)
                    .event(EXTERNALIZED_COMPUTE_CLUSTER_DELETE_AUX_CLUSTER_DELETE_STARTED).defaultFailureEvent()

                    .from(EXTERNALIZED_COMPUTE_CLUSTER_AUXILIARY_DELETE_IN_PROGRESS_STATE)
                    .to(EXTERNALIZED_COMPUTE_CLUSTER_DELETE_STATE)
                    .event(EXTERNALIZED_COMPUTE_CLUSTER_DELETE_INITIATED_EVENT).defaultFailureEvent()

                    .from(EXTERNALIZED_COMPUTE_CLUSTER_DELETE_STATE)
                    .to(EXTERNALIZED_COMPUTE_CLUSTER_DELETE_IN_PROGRESS_STATE)
                    .event(EXTERNALIZED_COMPUTE_CLUSTER_DELETE_STARTED_EVENT).defaultFailureEvent()

                    .from(EXTERNALIZED_COMPUTE_CLUSTER_DELETE_IN_PROGRESS_STATE)
                    .to(EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINISHED_STATE)
                    .event(EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINISHED_EVENT).defaultFailureEvent()

                    .from(EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINALIZED_EVENT).defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<ExternalizedComputeClusterDeleteState, ExternalizedComputeClusterDeleteFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
                    EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAILED_STATE, EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAIL_HANDLED_EVENT);

    public ExternalizedComputeClusterDeleteFlowConfig() {
        super(ExternalizedComputeClusterDeleteState.class, ExternalizedComputeClusterDeleteFlowEvent.class);
    }

    public ExternalizedComputeClusterDeleteFlowEvent[] getEvents() {
        return ExternalizedComputeClusterDeleteFlowEvent.values();
    }

    @Override
    public ExternalizedComputeClusterDeleteFlowEvent[] getInitEvents() {
        return new ExternalizedComputeClusterDeleteFlowEvent[] {
                EXTERNALIZED_COMPUTE_CLUSTER_DELETE_AUX_CLUSTER_DELETE_INITIATED_EVENT,
                EXTERNALIZED_COMPUTE_CLUSTER_DELETE_INITIATED_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Delete Externalized Compute";
    }

    @Override
    protected List<Transition<ExternalizedComputeClusterDeleteState, ExternalizedComputeClusterDeleteFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ExternalizedComputeClusterDeleteState, ExternalizedComputeClusterDeleteFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ExternalizedComputeClusterDeleteFlowEvent getRetryableEvent() {
        return EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAIL_HANDLED_EVENT;
    }

    @Override
    public OperationType getFlowOperationType() {
        return OperationType.PROVISION;
    }

}
