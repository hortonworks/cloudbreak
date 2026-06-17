package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade;

import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_FAILURE_CLEANUP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_LB_CONFIGURATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_LB_DB_CLEANUP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_LB_DELETED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_LB_PROVISIONED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_METADATA_COLLECTED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeState.PREPARE_UPGRADE_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeState.PREPARE_UPGRADE_FAILURE_CLEANUP_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeState.PREPARE_UPGRADE_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeState.PREPARE_UPGRADE_LB_CONFIGURATION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeState.PREPARE_UPGRADE_LB_DB_CLEANUP_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeState.PREPARE_UPGRADE_LB_DELETION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeState.PREPARE_UPGRADE_LB_PROVISION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeState.PREPARE_UPGRADE_METADATA_COLLECTION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class PrepareUpgradeFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<PrepareUpgradeState, PrepareUpgradeEvent>
        implements RetryableFlowConfiguration<PrepareUpgradeEvent> {

    private static final PrepareUpgradeEvent[] INIT_EVENTS = {PREPARE_UPGRADE_EVENT};

    private static final FlowEdgeConfig<PrepareUpgradeState, PrepareUpgradeEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, PREPARE_UPGRADE_FAILURE_CLEANUP_STATE, PREPARE_UPGRADE_FAILURE_HANDLED_EVENT);

    private static final List<Transition<PrepareUpgradeState, PrepareUpgradeEvent>> TRANSITIONS =
            new Builder<PrepareUpgradeState, PrepareUpgradeEvent>().defaultFailureEvent(PREPARE_UPGRADE_FAILURE_EVENT)

            .from(INIT_STATE)
            .to(PREPARE_UPGRADE_LB_CONFIGURATION_STATE)
            .event(PREPARE_UPGRADE_EVENT)
            .noFailureEvent()

            .from(PREPARE_UPGRADE_LB_CONFIGURATION_STATE)
            .to(PREPARE_UPGRADE_LB_PROVISION_STATE)
            .event(PREPARE_UPGRADE_LB_CONFIGURATION_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(PREPARE_UPGRADE_LB_CONFIGURATION_STATE)
            .to(PREPARE_UPGRADE_FINISHED_STATE)
            .event(PREPARE_UPGRADE_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(PREPARE_UPGRADE_LB_PROVISION_STATE)
            .to(PREPARE_UPGRADE_METADATA_COLLECTION_STATE)
            .event(PREPARE_UPGRADE_LB_PROVISIONED_EVENT)
            .defaultFailureEvent()

            .from(PREPARE_UPGRADE_METADATA_COLLECTION_STATE)
            .to(PREPARE_UPGRADE_LB_DELETION_STATE)
            .event(PREPARE_UPGRADE_METADATA_COLLECTED_EVENT)
            .defaultFailureEvent()

            .from(PREPARE_UPGRADE_LB_DELETION_STATE)
            .to(PREPARE_UPGRADE_LB_DB_CLEANUP_STATE)
            .event(PREPARE_UPGRADE_LB_DELETED_EVENT)
            .defaultFailureEvent()

            .from(PREPARE_UPGRADE_LB_DB_CLEANUP_STATE)
            .to(PREPARE_UPGRADE_FINISHED_STATE)
            .event(PREPARE_UPGRADE_LB_DB_CLEANUP_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(PREPARE_UPGRADE_FAILURE_CLEANUP_STATE)
            .to(PREPARE_UPGRADE_FAILED_STATE)
            .event(PREPARE_UPGRADE_FAILURE_CLEANUP_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(PREPARE_UPGRADE_FAILED_STATE)
            .to(FINAL_STATE)
            .event(PREPARE_UPGRADE_FAILURE_HANDLED_EVENT)
            .noFailureEvent()

            .from(PREPARE_UPGRADE_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(PREPARE_UPGRADE_FINALIZED_EVENT)
            .defaultFailureEvent()

            .build();

    public PrepareUpgradeFlowConfig() {
        super(PrepareUpgradeState.class, PrepareUpgradeEvent.class);
    }

    @Override
    protected List<Transition<PrepareUpgradeState, PrepareUpgradeEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<PrepareUpgradeState, PrepareUpgradeEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public PrepareUpgradeEvent[] getEvents() {
        return PrepareUpgradeEvent.values();
    }

    @Override
    public PrepareUpgradeEvent[] getInitEvents() {
        return INIT_EVENTS;
    }

    @Override
    public String getDisplayName() {
        return "Prepare FreeIPA upgrade";
    }

    @Override
    public PrepareUpgradeEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }
}
