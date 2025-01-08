package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeEvent.VALIDATE_RDS_UPGRADE_BACKUP_VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeEvent.VALIDATE_RDS_UPGRADE_CLEANUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeEvent.VALIDATE_RDS_UPGRADE_CONNECTION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeEvent.VALIDATE_RDS_UPGRADE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeEvent.VALIDATE_RDS_UPGRADE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeEvent.VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeEvent.VALIDATE_RDS_UPGRADE_PUSH_SALT_STATES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeEvent.WAIT_FOR_VALIDATE_RDS_UPGRADE_CLEANUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeEvent.WAIT_FOR_VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeState.VALIDATE_RDS_UPGRADE_BACKUP_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeState.VALIDATE_RDS_UPGRADE_CLEANUP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeState.VALIDATE_RDS_UPGRADE_CONNECTION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeState.VALIDATE_RDS_UPGRADE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeState.VALIDATE_RDS_UPGRADE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeState.VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeState.VALIDATE_RDS_UPGRADE_PUSH_SALT_STATES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeState.WAIT_FOR_VALIDATE_RDS_UPGRADE_CLEANUP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeState.WAIT_FOR_VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ValidateRdsUpgradeFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<ValidateRdsUpgradeState, ValidateRdsUpgradeEvent>
        implements RetryableFlowConfiguration<ValidateRdsUpgradeEvent> {

    private static final List<Transition<ValidateRdsUpgradeState, ValidateRdsUpgradeEvent>> TRANSITIONS =
            new Transition.Builder<ValidateRdsUpgradeState, ValidateRdsUpgradeEvent>()
                    .defaultFailureEvent(VALIDATE_RDS_UPGRADE_FAILED_EVENT)
                    .from(INIT_STATE).to(VALIDATE_RDS_UPGRADE_PUSH_SALT_STATES_STATE)
                    .event(VALIDATE_RDS_UPGRADE_EVENT)
                    .defaultFailureEvent()

                    .from(VALIDATE_RDS_UPGRADE_PUSH_SALT_STATES_STATE).to(VALIDATE_RDS_UPGRADE_BACKUP_VALIDATION_STATE)
                    .event(VALIDATE_RDS_UPGRADE_PUSH_SALT_STATES_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(VALIDATE_RDS_UPGRADE_BACKUP_VALIDATION_STATE).to(VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_STATE)
                    .event(VALIDATE_RDS_UPGRADE_BACKUP_VALIDATION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_STATE).to(WAIT_FOR_VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_STATE)
                    .event(VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(WAIT_FOR_VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_STATE).to(VALIDATE_RDS_UPGRADE_CONNECTION_STATE)
                    .event(WAIT_FOR_VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(VALIDATE_RDS_UPGRADE_CONNECTION_STATE).to(VALIDATE_RDS_UPGRADE_CLEANUP_STATE)
                    .event(VALIDATE_RDS_UPGRADE_CONNECTION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(VALIDATE_RDS_UPGRADE_CLEANUP_STATE).to(WAIT_FOR_VALIDATE_RDS_UPGRADE_CLEANUP_STATE)
                    .event(VALIDATE_RDS_UPGRADE_CLEANUP_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(WAIT_FOR_VALIDATE_RDS_UPGRADE_CLEANUP_STATE).to(VALIDATE_RDS_UPGRADE_FINISHED_STATE)
                    .event(WAIT_FOR_VALIDATE_RDS_UPGRADE_CLEANUP_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(VALIDATE_RDS_UPGRADE_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<ValidateRdsUpgradeState, ValidateRdsUpgradeEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, VALIDATE_RDS_UPGRADE_FAILED_STATE, FAIL_HANDLED_EVENT);

    public ValidateRdsUpgradeFlowConfig() {
        super(ValidateRdsUpgradeState.class, ValidateRdsUpgradeEvent.class);
    }

    @Override
    protected List<Transition<ValidateRdsUpgradeState, ValidateRdsUpgradeEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ValidateRdsUpgradeState, ValidateRdsUpgradeEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ValidateRdsUpgradeEvent[] getEvents() {
        return ValidateRdsUpgradeEvent.values();
    }

    @Override
    public ValidateRdsUpgradeEvent[] getInitEvents() {
        return new ValidateRdsUpgradeEvent[]{
                VALIDATE_RDS_UPGRADE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "RDS upgrade validation";
    }

    @Override
    public ValidateRdsUpgradeEvent getRetryableEvent() {
        return FAIL_HANDLED_EVENT;
    }
}