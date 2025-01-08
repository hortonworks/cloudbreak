package com.sequenceiq.redbeams.flow.redbeams.upgrade;

import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeEvent.REDBEAMS_START_VALIDATE_UPGRADE_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeEvent.REDBEAMS_VALIDATE_UPGRADE_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeEvent.REDBEAMS_VALIDATE_UPGRADE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeEvent.REDBEAMS_VALIDATE_UPGRADE_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeEvent.VALIDATE_UPGRADE_DATABASE_SERVER_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeState.FINAL_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeState.INIT_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeState.REDBEAMS_VALIDATE_UPGRADE_FAILED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeState.REDBEAMS_VALIDATE_UPGRADE_FINISHED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeState.VALIDATE_UPGRADE_DATABASE_SERVER_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class RedbeamsValidateUpgradeFlowConfig extends AbstractFlowConfiguration<RedbeamsValidateUpgradeState, RedbeamsValidateUpgradeEvent>
        implements RetryableFlowConfiguration<RedbeamsValidateUpgradeEvent> {

    private static final RedbeamsValidateUpgradeEvent[] REDBEAMS_INIT_EVENTS = {REDBEAMS_START_VALIDATE_UPGRADE_EVENT};

    private static final List<Transition<RedbeamsValidateUpgradeState, RedbeamsValidateUpgradeEvent>> TRANSITIONS =
            new Transition.Builder<RedbeamsValidateUpgradeState, RedbeamsValidateUpgradeEvent>().defaultFailureEvent(REDBEAMS_VALIDATE_UPGRADE_FAILED_EVENT)
                    .from(INIT_STATE)
                    .to(VALIDATE_UPGRADE_DATABASE_SERVER_STATE).event(REDBEAMS_START_VALIDATE_UPGRADE_EVENT).defaultFailureEvent()

                    .from(VALIDATE_UPGRADE_DATABASE_SERVER_STATE)
                    .to(REDBEAMS_VALIDATE_UPGRADE_FINISHED_STATE).event(VALIDATE_UPGRADE_DATABASE_SERVER_FINISHED_EVENT).defaultFailureEvent()

                    .from(REDBEAMS_VALIDATE_UPGRADE_FINISHED_STATE)
                    .to(FINAL_STATE).event(REDBEAMS_VALIDATE_UPGRADE_FINISHED_EVENT).defaultFailureEvent()

                    .from(REDBEAMS_VALIDATE_UPGRADE_FAILED_STATE)
                    .to(FINAL_STATE).event(REDBEAMS_VALIDATE_UPGRADE_FAILURE_HANDLED_EVENT).noFailureEvent()
                    .build();

    private static final FlowEdgeConfig<RedbeamsValidateUpgradeState, RedbeamsValidateUpgradeEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, REDBEAMS_VALIDATE_UPGRADE_FAILED_STATE, REDBEAMS_VALIDATE_UPGRADE_FAILURE_HANDLED_EVENT);

    public RedbeamsValidateUpgradeFlowConfig() {
        super(RedbeamsValidateUpgradeState.class, RedbeamsValidateUpgradeEvent.class);
    }

    @Override
    protected List<Transition<RedbeamsValidateUpgradeState, RedbeamsValidateUpgradeEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<RedbeamsValidateUpgradeState, RedbeamsValidateUpgradeEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public RedbeamsValidateUpgradeEvent[] getEvents() {
        return RedbeamsValidateUpgradeEvent.values();
    }

    @Override
    public RedbeamsValidateUpgradeEvent[] getInitEvents() {
        return REDBEAMS_INIT_EVENTS;
    }

    @Override
    public String getDisplayName() {
        return "Validation of the upgrade of database server";
    }

    @Override
    public RedbeamsValidateUpgradeEvent getRetryableEvent() {
        return REDBEAMS_VALIDATE_UPGRADE_FAILURE_HANDLED_EVENT;
    }

}