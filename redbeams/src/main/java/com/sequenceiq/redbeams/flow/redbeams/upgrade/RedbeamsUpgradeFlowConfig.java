package com.sequenceiq.redbeams.flow.redbeams.upgrade;

import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent.BACKUP_DATABASE_SERVER_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent.REDBEAMS_START_UPGRADE_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent.REDBEAMS_UPGRADE_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent.REDBEAMS_UPGRADE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent.REDBEAMS_UPGRADE_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent.RESTORE_DATABASE_SERVER_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent.UPGRADE_DATABASE_SERVER_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeState.BACKUP_DATABASE_SERVER_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeState.FINAL_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeState.INIT_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeState.REDBEAMS_UPGRADE_FAILED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeState.REDBEAMS_UPGRADE_FINISHED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeState.RESTORE_DATABASE_SERVER_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeState.UPGRADE_DATABASE_SERVER_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class RedbeamsUpgradeFlowConfig extends AbstractFlowConfiguration<RedbeamsUpgradeState, RedbeamsUpgradeEvent>
        implements RetryableFlowConfiguration<RedbeamsUpgradeEvent> {

    private static final RedbeamsUpgradeEvent[] REDBEAMS_INIT_EVENTS = {REDBEAMS_START_UPGRADE_EVENT};

    private static final List<Transition<RedbeamsUpgradeState, RedbeamsUpgradeEvent>> TRANSITIONS =
            new Transition.Builder<RedbeamsUpgradeState, RedbeamsUpgradeEvent>().defaultFailureEvent(REDBEAMS_UPGRADE_FAILED_EVENT)
                    .from(INIT_STATE).to(UPGRADE_DATABASE_SERVER_STATE).event(REDBEAMS_START_UPGRADE_EVENT).defaultFailureEvent()
                    .from(BACKUP_DATABASE_SERVER_STATE).to(UPGRADE_DATABASE_SERVER_STATE).event(BACKUP_DATABASE_SERVER_FINISHED_EVENT).defaultFailureEvent()
                    .from(UPGRADE_DATABASE_SERVER_STATE).to(REDBEAMS_UPGRADE_FINISHED_STATE).event(UPGRADE_DATABASE_SERVER_FINISHED_EVENT).defaultFailureEvent()
                    .from(RESTORE_DATABASE_SERVER_STATE).to(REDBEAMS_UPGRADE_FINISHED_STATE).event(RESTORE_DATABASE_SERVER_FINISHED_EVENT).defaultFailureEvent()
                    .from(REDBEAMS_UPGRADE_FINISHED_STATE).to(FINAL_STATE).event(REDBEAMS_UPGRADE_FINISHED_EVENT).defaultFailureEvent()
                    .from(REDBEAMS_UPGRADE_FAILED_STATE).to(FINAL_STATE).event(REDBEAMS_UPGRADE_FAILURE_HANDLED_EVENT).noFailureEvent()
                    .build();

    private static final FlowEdgeConfig<RedbeamsUpgradeState, RedbeamsUpgradeEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, REDBEAMS_UPGRADE_FAILED_STATE, REDBEAMS_UPGRADE_FAILURE_HANDLED_EVENT);

    public RedbeamsUpgradeFlowConfig() {
        super(RedbeamsUpgradeState.class, RedbeamsUpgradeEvent.class);
    }

    @Override
    protected List<Transition<RedbeamsUpgradeState, RedbeamsUpgradeEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<RedbeamsUpgradeState, RedbeamsUpgradeEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public RedbeamsUpgradeEvent[] getEvents() {
        return RedbeamsUpgradeEvent.values();
    }

    @Override
    public RedbeamsUpgradeEvent[] getInitEvents() {
        return REDBEAMS_INIT_EVENTS;
    }

    @Override
    public String getDisplayName() {
        return "Upgrade database server";
    }

    @Override
    public RedbeamsUpgradeEvent getRetryableEvent() {
        return REDBEAMS_UPGRADE_FAILURE_HANDLED_EVENT;
    }

}