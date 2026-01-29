package com.sequenceiq.datalake.flow.datalake.recovery;

import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryEvent.DATALAKE_RECOVERY_COULD_NOT_START_EVENT;
import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryEvent.DATALAKE_RECOVERY_EVENT;
import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryEvent.DATALAKE_RECOVERY_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryEvent.DATALAKE_RECOVERY_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryEvent.DATALAKE_RECOVERY_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryEvent.DATALAKE_RECOVERY_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryEvent.DATALAKE_RECOVERY_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryState.DATALAKE_RECOVERY_COULD_NOT_START_STATE;
import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryState.DATALAKE_RECOVERY_FAILED_STATE;
import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryState.DATALAKE_RECOVERY_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryState.DATALAKE_RECOVERY_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryState.DATALAKE_RECOVERY_START_STATE;
import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class DatalakeUpgradeRecoveryFlowConfig extends AbstractFlowConfiguration<DatalakeUpgradeRecoveryState, DatalakeUpgradeRecoveryEvent>
        implements RetryableDatalakeFlowConfiguration<DatalakeUpgradeRecoveryEvent> {

    private static final List<Transition<DatalakeUpgradeRecoveryState, DatalakeUpgradeRecoveryEvent>> TRANSITIONS =
            new Transition.Builder<DatalakeUpgradeRecoveryState, DatalakeUpgradeRecoveryEvent>()
                    .defaultFailureEvent(DATALAKE_RECOVERY_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(DATALAKE_RECOVERY_START_STATE)
                    .event(DATALAKE_RECOVERY_EVENT).noFailureEvent()

                    .from(DATALAKE_RECOVERY_START_STATE)
                    .to(DATALAKE_RECOVERY_IN_PROGRESS_STATE)
                    .event(DATALAKE_RECOVERY_IN_PROGRESS_EVENT)
                    .failureState(DATALAKE_RECOVERY_COULD_NOT_START_STATE)
                    .failureEvent(DATALAKE_RECOVERY_COULD_NOT_START_EVENT)

                    .from(DATALAKE_RECOVERY_IN_PROGRESS_STATE)
                    .to(DATALAKE_RECOVERY_FINISHED_STATE)
                    .event(DATALAKE_RECOVERY_SUCCESS_EVENT).defaultFailureEvent()

                    .from(DATALAKE_RECOVERY_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_RECOVERY_FINALIZED_EVENT).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<DatalakeUpgradeRecoveryState, DatalakeUpgradeRecoveryEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATALAKE_RECOVERY_FAILED_STATE, DATALAKE_RECOVERY_FAILED_HANDLED_EVENT);

    public DatalakeUpgradeRecoveryFlowConfig() {
        super(DatalakeUpgradeRecoveryState.class, DatalakeUpgradeRecoveryEvent.class);
    }

    @Override
    public DatalakeUpgradeRecoveryEvent[] getEvents() {
        return DatalakeUpgradeRecoveryEvent.values();
    }

    @Override
    public DatalakeUpgradeRecoveryEvent[] getInitEvents() {
        return new DatalakeUpgradeRecoveryEvent[]{
                DATALAKE_RECOVERY_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Recover SDX failure";
    }

    @Override
    protected List<Transition<DatalakeUpgradeRecoveryState, DatalakeUpgradeRecoveryEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatalakeUpgradeRecoveryState, DatalakeUpgradeRecoveryEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DatalakeUpgradeRecoveryEvent getRetryableEvent() {
        return DATALAKE_RECOVERY_FAILED_HANDLED_EVENT;
    }
}
