package com.sequenceiq.redbeams.flow.redbeams.upgrade;

import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupEvent.REDBEAMS_START_VALIDATE_UPGRADE_CLEANUP_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupEvent.REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupEvent.REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupEvent.REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupEvent.VALIDATE_UPGRADE_CLEANUP_DATABASE_SERVER_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupState.FINAL_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupState.INIT_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupState.REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FAILED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupState.REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FINISHED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupState.VALIDATE_UPGRADE_DATABASE_SERVER_CLEANUP_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class RedbeamsValidateUpgradeCleanupFlowConfig extends AbstractFlowConfiguration<RedbeamsValidateUpgradeCleanupState, RedbeamsValidateUpgradeCleanupEvent>
        implements RetryableFlowConfiguration<RedbeamsValidateUpgradeCleanupEvent> {

    private static final RedbeamsValidateUpgradeCleanupEvent[] REDBEAMS_INIT_EVENTS = {REDBEAMS_START_VALIDATE_UPGRADE_CLEANUP_EVENT};

    private static final List<Transition<RedbeamsValidateUpgradeCleanupState, RedbeamsValidateUpgradeCleanupEvent>> TRANSITIONS =
            new Transition.Builder<RedbeamsValidateUpgradeCleanupState,
                    RedbeamsValidateUpgradeCleanupEvent>().defaultFailureEvent(REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FAILED_EVENT)
                    .from(INIT_STATE)
                    .to(VALIDATE_UPGRADE_DATABASE_SERVER_CLEANUP_STATE)
                    .event(REDBEAMS_START_VALIDATE_UPGRADE_CLEANUP_EVENT).defaultFailureEvent()

                    .from(VALIDATE_UPGRADE_DATABASE_SERVER_CLEANUP_STATE)
                    .to(REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FINISHED_STATE)
                    .event(VALIDATE_UPGRADE_CLEANUP_DATABASE_SERVER_FINISHED_EVENT).defaultFailureEvent()

                    .from(REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FINISHED_EVENT).defaultFailureEvent()

                    .from(REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FAILED_STATE)
                    .to(FINAL_STATE)
                    .event(REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FAILURE_HANDLED_EVENT).noFailureEvent()
                    .build();

    private static final FlowEdgeConfig<RedbeamsValidateUpgradeCleanupState, RedbeamsValidateUpgradeCleanupEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE,
                    FINAL_STATE,
                    REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FAILED_STATE,
                    REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FAILURE_HANDLED_EVENT);

    public RedbeamsValidateUpgradeCleanupFlowConfig() {
        super(RedbeamsValidateUpgradeCleanupState.class, RedbeamsValidateUpgradeCleanupEvent.class);
    }

    @Override
    protected List<Transition<RedbeamsValidateUpgradeCleanupState, RedbeamsValidateUpgradeCleanupEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<RedbeamsValidateUpgradeCleanupState, RedbeamsValidateUpgradeCleanupEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public RedbeamsValidateUpgradeCleanupEvent[] getEvents() {
        return RedbeamsValidateUpgradeCleanupEvent.values();
    }

    @Override
    public RedbeamsValidateUpgradeCleanupEvent[] getInitEvents() {
        return REDBEAMS_INIT_EVENTS;
    }

    @Override
    public String getDisplayName() {
        return "Cleaning up the validation of the upgrade of database server";
    }

    @Override
    public RedbeamsValidateUpgradeCleanupEvent getRetryableEvent() {
        return REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FAILURE_HANDLED_EVENT;
    }

}