package com.sequenceiq.externalizedcompute.flow.delete;

import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINALIZED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINISHED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_INITIATED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_STARTED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteState.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAILED_STATE;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteState.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINISHED_STATE;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteState.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_IN_PROGRESS_STATE;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteState.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_STATE;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteState.FINAL_STATE;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteState.INIT_STATE;

import java.util.List;

import org.springframework.context.annotation.Configuration;

import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Configuration
public class ExternalizedComputeClusterDeleteFlowConfig
        extends AbstractFlowConfiguration<ExternalizedComputeClusterDeleteState, ExternalizedComputeClusterDeleteEvent>
        implements RetryableFlowConfiguration<ExternalizedComputeClusterDeleteEvent> {

    private static final List<Transition<ExternalizedComputeClusterDeleteState, ExternalizedComputeClusterDeleteEvent>> TRANSITIONS =
            new Transition.Builder<ExternalizedComputeClusterDeleteState, ExternalizedComputeClusterDeleteEvent>()
                    .defaultFailureEvent(ExternalizedComputeClusterDeleteEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAILED_EVENT)
                    .from(INIT_STATE)
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

    private static final FlowEdgeConfig<ExternalizedComputeClusterDeleteState, ExternalizedComputeClusterDeleteEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
                    EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAILED_STATE, EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAIL_HANDLED_EVENT);

    public ExternalizedComputeClusterDeleteFlowConfig() {
        super(ExternalizedComputeClusterDeleteState.class, ExternalizedComputeClusterDeleteEvent.class);
    }

    public ExternalizedComputeClusterDeleteEvent[] getEvents() {
        return ExternalizedComputeClusterDeleteEvent.values();
    }

    @Override
    public ExternalizedComputeClusterDeleteEvent[] getInitEvents() {
        return new ExternalizedComputeClusterDeleteEvent[]{
                EXTERNALIZED_COMPUTE_CLUSTER_DELETE_INITIATED_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Delete Externalized Compute";
    }

    @Override
    protected List<Transition<ExternalizedComputeClusterDeleteState, ExternalizedComputeClusterDeleteEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ExternalizedComputeClusterDeleteState, ExternalizedComputeClusterDeleteEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ExternalizedComputeClusterDeleteEvent getRetryableEvent() {
        return EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAIL_HANDLED_EVENT;
    }

    @Override
    public OperationType getFlowOperationType() {
        return OperationType.PROVISION;
    }

}
