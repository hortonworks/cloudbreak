package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.ClusterUpgradeValidationState.CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.ClusterUpgradeValidationState.CLUSTER_UPGRADE_IMAGE_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.ClusterUpgradeValidationState.CLUSTER_UPGRADE_SERVICE_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.ClusterUpgradeValidationState.CLUSTER_UPGRADE_VALIDATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.ClusterUpgradeValidationState.CLUSTER_UPGRADE_VALIDATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.ClusterUpgradeValidationState.CLUSTER_UPGRADE_VALIDATION_INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.ClusterUpgradeValidationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.ClusterUpgradeValidationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FINALIZE_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FINISH_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FINISH_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.HANDLED_FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_IMAGE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.ClusterUpgradeValidationState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ClusterUpgradeValidationFlowConfig extends AbstractFlowConfiguration<ClusterUpgradeValidationState, ClusterUpgradeValidationStateSelectors>
        implements RetryableFlowConfiguration<ClusterUpgradeValidationStateSelectors> {

    private static final List<Transition<ClusterUpgradeValidationState, ClusterUpgradeValidationStateSelectors>> TRANSITIONS =
            new Transition.Builder<ClusterUpgradeValidationState, ClusterUpgradeValidationStateSelectors>()
                    .defaultFailureEvent(FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT)

                    .from(INIT_STATE).to(CLUSTER_UPGRADE_VALIDATION_INIT_STATE)
                    .event(START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT)
                    .defaultFailureEvent()

                    .from(CLUSTER_UPGRADE_VALIDATION_INIT_STATE).to(CLUSTER_UPGRADE_IMAGE_VALIDATION_STATE)
                    .event(START_CLUSTER_UPGRADE_IMAGE_VALIDATION_EVENT)
                    .defaultFailureEvent()

                    .from(CLUSTER_UPGRADE_IMAGE_VALIDATION_STATE).to(CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_STATE)
                    .event(START_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT)
                    .defaultFailureEvent()

                    .from(CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_STATE).to(CLUSTER_UPGRADE_SERVICE_VALIDATION_STATE)
                    .event(FINISH_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT)
                    .defaultFailureEvent()

                    .from(CLUSTER_UPGRADE_SERVICE_VALIDATION_STATE).to(CLUSTER_UPGRADE_VALIDATION_FINISHED_STATE)
                    .event(FINISH_CLUSTER_UPGRADE_VALIDATION_EVENT)
                    .defaultFailureEvent()

                    .from(CLUSTER_UPGRADE_VALIDATION_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZE_CLUSTER_UPGRADE_VALIDATION_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<ClusterUpgradeValidationState, ClusterUpgradeValidationStateSelectors> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE,
            FINAL_STATE, CLUSTER_UPGRADE_VALIDATION_FAILED_STATE, HANDLED_FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT);

    protected ClusterUpgradeValidationFlowConfig() {
        super(ClusterUpgradeValidationState.class, ClusterUpgradeValidationStateSelectors.class);
    }

    @Override
    protected List<Transition<ClusterUpgradeValidationState, ClusterUpgradeValidationStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ClusterUpgradeValidationState, ClusterUpgradeValidationStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterUpgradeValidationStateSelectors[] getEvents() {
        return ClusterUpgradeValidationStateSelectors.values();
    }

    @Override
    public ClusterUpgradeValidationStateSelectors[] getInitEvents() {
        return new ClusterUpgradeValidationStateSelectors[] {
                START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Cluster upgrade validation flow";
    }

    @Override
    public ClusterUpgradeValidationStateSelectors getRetryableEvent() {
        return ClusterUpgradeValidationStateSelectors.HANDLED_FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;
    }
}
