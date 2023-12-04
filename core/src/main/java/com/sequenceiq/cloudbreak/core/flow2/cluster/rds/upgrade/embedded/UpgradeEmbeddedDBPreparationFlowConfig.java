package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded.UpgradeEmbeddedDBPreparationEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded.UpgradeEmbeddedDBPreparationEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded.UpgradeEmbeddedDBPreparationEvent.UPGRADE_EMBEDDEDDB_PREPARATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded.UpgradeEmbeddedDBPreparationEvent.UPGRADE_EMBEDDEDDB_PREPARATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded.UpgradeEmbeddedDBPreparationEvent.UPGRADE_EMBEDDEDDB_PREPARATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded.UpgradeEmbeddedDBPreparationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded.UpgradeEmbeddedDBPreparationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded.UpgradeEmbeddedDBPreparationState.UPGRADE_EMBEDDED_DB_PREPARATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded.UpgradeEmbeddedDBPreparationState.UPGRADE_EMBEDDED_DB_PREPARATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded.UpgradeEmbeddedDBPreparationState.UPGRADE_EMBEDDED_DB_PREPARATION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class UpgradeEmbeddedDBPreparationFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<UpgradeEmbeddedDBPreparationState, UpgradeEmbeddedDBPreparationEvent>
        implements RetryableFlowConfiguration<UpgradeEmbeddedDBPreparationEvent> {

    private static final List<Transition<UpgradeEmbeddedDBPreparationState, UpgradeEmbeddedDBPreparationEvent>> TRANSITIONS =
            new Builder<UpgradeEmbeddedDBPreparationState, UpgradeEmbeddedDBPreparationEvent>()
                    .defaultFailureEvent(UPGRADE_EMBEDDEDDB_PREPARATION_FAILED_EVENT)
                    .from(INIT_STATE).to(UPGRADE_EMBEDDED_DB_PREPARATION_STATE)
                    .event(UPGRADE_EMBEDDEDDB_PREPARATION_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_EMBEDDED_DB_PREPARATION_STATE).to(UPGRADE_EMBEDDED_DB_PREPARATION_FINISHED_STATE)
                    .event(UPGRADE_EMBEDDEDDB_PREPARATION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_EMBEDDED_DB_PREPARATION_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<UpgradeEmbeddedDBPreparationState, UpgradeEmbeddedDBPreparationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, UPGRADE_EMBEDDED_DB_PREPARATION_FAILED_STATE, FAIL_HANDLED_EVENT);

    public UpgradeEmbeddedDBPreparationFlowConfig() {
        super(UpgradeEmbeddedDBPreparationState.class, UpgradeEmbeddedDBPreparationEvent.class);
    }

    @Override
    protected List<Transition<UpgradeEmbeddedDBPreparationState, UpgradeEmbeddedDBPreparationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<UpgradeEmbeddedDBPreparationState, UpgradeEmbeddedDBPreparationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public UpgradeEmbeddedDBPreparationEvent[] getEvents() {
        return UpgradeEmbeddedDBPreparationEvent.values();
    }

    @Override
    public UpgradeEmbeddedDBPreparationEvent[] getInitEvents() {
        return new UpgradeEmbeddedDBPreparationEvent[]{
                UPGRADE_EMBEDDEDDB_PREPARATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Embedded Database Upgrade Preparation";
    }

    @Override
    public UpgradeEmbeddedDBPreparationEvent getRetryableEvent() {
        return FAIL_HANDLED_EVENT;
    }
}
