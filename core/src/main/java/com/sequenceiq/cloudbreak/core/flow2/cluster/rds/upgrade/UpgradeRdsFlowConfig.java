package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsEvent.UPGRADE_RDS_DATA_BACKUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsEvent.UPGRADE_RDS_DATA_RESTORE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsEvent.UPGRADE_RDS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsEvent.UPGRADE_RDS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsEvent.UPGRADE_RDS_START_SERVICES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsEvent.UPGRADE_RDS_STOP_SERVICES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsEvent.UPGRADE_RDS_UPGRADE_DATABASE_SERVER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsState.UPGRADE_RDS_DATA_BACKUP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsState.UPGRADE_RDS_DATA_RESTORE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsState.UPGRADE_RDS_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsState.UPGRADE_RDS_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsState.UPGRADE_RDS_START_SERVICES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsState.UPGRADE_RDS_STOP_SERVICES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsState.UPGRADE_RDS_UPGRADE_DATABASE_SERVER_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class UpgradeRdsFlowConfig extends StackStatusFinalizerAbstractFlowConfig<UpgradeRdsState, UpgradeRdsEvent>
        implements RetryableFlowConfiguration<UpgradeRdsEvent> {

    private static final List<Transition<UpgradeRdsState, UpgradeRdsEvent>> TRANSITIONS =
            new Builder<UpgradeRdsState, UpgradeRdsEvent>()
                    .defaultFailureEvent(UPGRADE_RDS_FAILED_EVENT)
                    .from(INIT_STATE).to(UPGRADE_RDS_STOP_SERVICES_STATE)
                    .event(UPGRADE_RDS_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_RDS_STOP_SERVICES_STATE).to(UPGRADE_RDS_DATA_BACKUP_STATE)
                    .event(UPGRADE_RDS_STOP_SERVICES_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_RDS_DATA_BACKUP_STATE).to(UPGRADE_RDS_UPGRADE_DATABASE_SERVER_STATE)
                    .event(UPGRADE_RDS_DATA_BACKUP_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_RDS_UPGRADE_DATABASE_SERVER_STATE).to(UPGRADE_RDS_DATA_RESTORE_STATE)
                    .event(UPGRADE_RDS_UPGRADE_DATABASE_SERVER_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_RDS_DATA_RESTORE_STATE).to(UPGRADE_RDS_START_SERVICES_STATE)
                    .event(UPGRADE_RDS_DATA_RESTORE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_RDS_START_SERVICES_STATE).to(UPGRADE_RDS_FINISHED_STATE)
                    .event(UPGRADE_RDS_START_SERVICES_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_RDS_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<UpgradeRdsState, UpgradeRdsEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            UPGRADE_RDS_FAILED_STATE, FAIL_HANDLED_EVENT);

    public UpgradeRdsFlowConfig() {
        super(UpgradeRdsState.class, UpgradeRdsEvent.class);
    }

    @Override
    public FlowTriggerCondition getFlowTriggerCondition() {
        return getApplicationContext().getBean(RdsUpgradeFlowTriggerCondition.class);
    }

    @Override
    protected List<Transition<UpgradeRdsState, UpgradeRdsEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<UpgradeRdsState, UpgradeRdsEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public UpgradeRdsEvent[] getEvents() {
        return UpgradeRdsEvent.values();
    }

    @Override
    public UpgradeRdsEvent[] getInitEvents() {
        return new UpgradeRdsEvent[] {
                UPGRADE_RDS_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "RDS upgrade";
    }

    @Override
    public UpgradeRdsEvent getRetryableEvent() {
        return FAIL_HANDLED_EVENT;
    }
}
