package com.sequenceiq.externalizedcompute.flow.create;

import static com.sequenceiq.externalizedcompute.flow.create.ExternalizedComputeClusterCreateFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.create.ExternalizedComputeClusterCreateFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FINALIZED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.create.ExternalizedComputeClusterCreateFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FINISHED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.create.ExternalizedComputeClusterCreateFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_CREATE_INITIATED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.create.ExternalizedComputeClusterCreateFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_CREATE_WAIT_FINISHED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.create.ExternalizedComputeClusterCreateState.EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FAILED_STATE;
import static com.sequenceiq.externalizedcompute.flow.create.ExternalizedComputeClusterCreateState.EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FINISHED_STATE;
import static com.sequenceiq.externalizedcompute.flow.create.ExternalizedComputeClusterCreateState.EXTERNALIZED_COMPUTE_CLUSTER_CREATE_IN_PROGRESS_STATE;
import static com.sequenceiq.externalizedcompute.flow.create.ExternalizedComputeClusterCreateState.EXTERNALIZED_COMPUTE_CLUSTER_CREATE_WAIT_ENV_STATE;
import static com.sequenceiq.externalizedcompute.flow.create.ExternalizedComputeClusterCreateState.FINAL_STATE;
import static com.sequenceiq.externalizedcompute.flow.create.ExternalizedComputeClusterCreateState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ExternalizedComputeClusterCreateFlowConfig
        extends AbstractFlowConfiguration<ExternalizedComputeClusterCreateState, ExternalizedComputeClusterCreateFlowEvent>
        implements RetryableFlowConfiguration<ExternalizedComputeClusterCreateFlowEvent> {

    private static final List<Transition<ExternalizedComputeClusterCreateState, ExternalizedComputeClusterCreateFlowEvent>> TRANSITIONS =
            new Transition.Builder<ExternalizedComputeClusterCreateState, ExternalizedComputeClusterCreateFlowEvent>()
                    .defaultFailureEvent(ExternalizedComputeClusterCreateFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FAILED_EVENT)
                    .from(INIT_STATE)
                    .to(EXTERNALIZED_COMPUTE_CLUSTER_CREATE_WAIT_ENV_STATE)
                    .event(EXTERNALIZED_COMPUTE_CLUSTER_CREATE_INITIATED_EVENT).defaultFailureEvent()

                    .from(EXTERNALIZED_COMPUTE_CLUSTER_CREATE_WAIT_ENV_STATE)
                    .to(EXTERNALIZED_COMPUTE_CLUSTER_CREATE_IN_PROGRESS_STATE)
                    .event(EXTERNALIZED_COMPUTE_CLUSTER_CREATE_WAIT_FINISHED_EVENT).defaultFailureEvent()

                    .from(EXTERNALIZED_COMPUTE_CLUSTER_CREATE_IN_PROGRESS_STATE)
                    .to(EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FINISHED_STATE)
                    .event(EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FINISHED_EVENT).defaultFailureEvent()

                    .from(EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FINALIZED_EVENT).defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<ExternalizedComputeClusterCreateState, ExternalizedComputeClusterCreateFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
                    EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FAILED_STATE, EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FAIL_HANDLED_EVENT);

    public ExternalizedComputeClusterCreateFlowConfig() {
        super(ExternalizedComputeClusterCreateState.class, ExternalizedComputeClusterCreateFlowEvent.class);
    }

    public ExternalizedComputeClusterCreateFlowEvent[] getEvents() {
        return ExternalizedComputeClusterCreateFlowEvent.values();
    }

    @Override
    public ExternalizedComputeClusterCreateFlowEvent[] getInitEvents() {
        return new ExternalizedComputeClusterCreateFlowEvent[]{
                EXTERNALIZED_COMPUTE_CLUSTER_CREATE_INITIATED_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Create Externalized Compute";
    }

    @Override
    protected List<Transition<ExternalizedComputeClusterCreateState, ExternalizedComputeClusterCreateFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ExternalizedComputeClusterCreateState, ExternalizedComputeClusterCreateFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ExternalizedComputeClusterCreateFlowEvent getRetryableEvent() {
        return EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FAIL_HANDLED_EVENT;
    }

    @Override
    public OperationType getFlowOperationType() {
        return OperationType.PROVISION;
    }
}
