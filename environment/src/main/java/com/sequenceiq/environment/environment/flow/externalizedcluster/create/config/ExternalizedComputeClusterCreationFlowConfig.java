package com.sequenceiq.environment.environment.flow.externalizedcluster.create.config;

import static com.sequenceiq.environment.environment.flow.externalizedcluster.create.ExternalizedComputeClusterCreationState.DEFAULT_COMPUTE_CLUSTER_CREATION_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.create.ExternalizedComputeClusterCreationState.DEFAULT_COMPUTE_CLUSTER_CREATION_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.create.ExternalizedComputeClusterCreationState.DEFAULT_COMPUTE_CLUSTER_CREATION_START_STATE;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.create.ExternalizedComputeClusterCreationState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.create.ExternalizedComputeClusterCreationState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationStateSelectors.DEFAULT_COMPUTE_CLUSTER_CREATION_FAILED_EVENT;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationStateSelectors.DEFAULT_COMPUTE_CLUSTER_CREATION_FAILED_HANDLED_EVENT;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationStateSelectors.DEFAULT_COMPUTE_CLUSTER_CREATION_FINALIZED_EVENT;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationStateSelectors.DEFAULT_COMPUTE_CLUSTER_CREATION_FINISHED_EVENT;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationStateSelectors.DEFAULT_COMPUTE_CLUSTER_CREATION_START_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.flow.externalizedcluster.create.ExternalizedComputeClusterCreationState;
import com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ExternalizedComputeClusterCreationFlowConfig
        extends AbstractFlowConfiguration<ExternalizedComputeClusterCreationState, ExternalizedComputeClusterCreationStateSelectors>
        implements RetryableFlowConfiguration<ExternalizedComputeClusterCreationStateSelectors> {

    private static final List<Transition<ExternalizedComputeClusterCreationState, ExternalizedComputeClusterCreationStateSelectors>> TRANSITIONS =
            new Transition.Builder<ExternalizedComputeClusterCreationState, ExternalizedComputeClusterCreationStateSelectors>()
                    .defaultFailureEvent(DEFAULT_COMPUTE_CLUSTER_CREATION_FAILED_EVENT)

                    .from(INIT_STATE).to(DEFAULT_COMPUTE_CLUSTER_CREATION_START_STATE)
                    .event(DEFAULT_COMPUTE_CLUSTER_CREATION_START_EVENT).defaultFailureEvent()

                    .from(DEFAULT_COMPUTE_CLUSTER_CREATION_START_STATE).to(DEFAULT_COMPUTE_CLUSTER_CREATION_FINISHED_STATE)
                    .event(DEFAULT_COMPUTE_CLUSTER_CREATION_FINISHED_EVENT).defaultFailureEvent()

                    .from(DEFAULT_COMPUTE_CLUSTER_CREATION_FINISHED_STATE).to(FINAL_STATE)
                    .event(DEFAULT_COMPUTE_CLUSTER_CREATION_FINALIZED_EVENT).defaultFailureEvent()

                    .build();

    protected ExternalizedComputeClusterCreationFlowConfig() {
        super(ExternalizedComputeClusterCreationState.class, ExternalizedComputeClusterCreationStateSelectors.class);
    }

    @Override
    protected List<Transition<ExternalizedComputeClusterCreationState, ExternalizedComputeClusterCreationStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ExternalizedComputeClusterCreationState, ExternalizedComputeClusterCreationStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DEFAULT_COMPUTE_CLUSTER_CREATION_FAILED_STATE,
                DEFAULT_COMPUTE_CLUSTER_CREATION_FAILED_HANDLED_EVENT);
    }

    @Override
    public ExternalizedComputeClusterCreationStateSelectors[] getEvents() {
        return ExternalizedComputeClusterCreationStateSelectors.values();
    }

    @Override
    public ExternalizedComputeClusterCreationStateSelectors[] getInitEvents() {
        return new ExternalizedComputeClusterCreationStateSelectors[]{DEFAULT_COMPUTE_CLUSTER_CREATION_START_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Create default compute cluster for environment";
    }

    @Override
    public ExternalizedComputeClusterCreationStateSelectors getRetryableEvent() {
        return DEFAULT_COMPUTE_CLUSTER_CREATION_FAILED_HANDLED_EVENT;
    }
}
