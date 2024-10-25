package com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.config;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.COMPUTE_CLUSTER_REINIT_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.COMPUTE_CLUSTER_REINIT_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.COMPUTE_CLUSTER_REINIT_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.UNSET;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.ExternalizedComputeClusterReInitializationState.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.ExternalizedComputeClusterReInitializationState.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.ExternalizedComputeClusterReInitializationState.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_START_STATE;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.ExternalizedComputeClusterReInitializationState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.ExternalizedComputeClusterReInitializationState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationStateSelectors.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_EVENT;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationStateSelectors.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_HANDLED_EVENT;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationStateSelectors.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINALIZED_EVENT;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationStateSelectors.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINISHED_EVENT;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationStateSelectors.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_START_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseAware;
import com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.ExternalizedComputeClusterReInitializationState;
import com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationStateSelectors;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ExternalizedComputeClusterReInitializationFlowConfig
        extends AbstractFlowConfiguration<ExternalizedComputeClusterReInitializationState, ExternalizedComputeClusterReInitializationStateSelectors>
        implements RetryableFlowConfiguration<ExternalizedComputeClusterReInitializationStateSelectors>, EnvironmentUseCaseAware {

    private static final List<Transition<ExternalizedComputeClusterReInitializationState, ExternalizedComputeClusterReInitializationStateSelectors>>
            TRANSITIONS = new Transition.Builder<ExternalizedComputeClusterReInitializationState, ExternalizedComputeClusterReInitializationStateSelectors>()
                    .defaultFailureEvent(DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_EVENT)

                    .from(INIT_STATE).to(DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_START_STATE)
                    .event(DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_START_EVENT).defaultFailureEvent()

                    .from(DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_START_STATE).to(DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINISHED_STATE)
                    .event(DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINISHED_EVENT).defaultFailureEvent()

                    .from(DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINISHED_STATE).to(FINAL_STATE)
                    .event(DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINALIZED_EVENT).defaultFailureEvent()

                    .build();

    protected ExternalizedComputeClusterReInitializationFlowConfig() {
        super(ExternalizedComputeClusterReInitializationState.class, ExternalizedComputeClusterReInitializationStateSelectors.class);
    }

    @Override
    protected List<Transition<ExternalizedComputeClusterReInitializationState, ExternalizedComputeClusterReInitializationStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ExternalizedComputeClusterReInitializationState, ExternalizedComputeClusterReInitializationStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_STATE,
                DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_HANDLED_EVENT);
    }

    @Override
    public ExternalizedComputeClusterReInitializationStateSelectors[] getEvents() {
        return ExternalizedComputeClusterReInitializationStateSelectors.values();
    }

    @Override
    public ExternalizedComputeClusterReInitializationStateSelectors[] getInitEvents() {
        return new ExternalizedComputeClusterReInitializationStateSelectors[] { DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_START_EVENT };
    }

    @Override
    public String getDisplayName() {
        return "Reinitialize default compute cluster for environment";
    }

    @Override
    public ExternalizedComputeClusterReInitializationStateSelectors getRetryableEvent() {
        return DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_HANDLED_EVENT;
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return COMPUTE_CLUSTER_REINIT_STARTED;
        } else if (DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINISHED_STATE.equals(flowState)) {
            return COMPUTE_CLUSTER_REINIT_FINISHED;
        } else if (DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_STATE.equals(flowState)) {
            return COMPUTE_CLUSTER_REINIT_FAILED;
        } else {
            return UNSET;
        }
    }
}
