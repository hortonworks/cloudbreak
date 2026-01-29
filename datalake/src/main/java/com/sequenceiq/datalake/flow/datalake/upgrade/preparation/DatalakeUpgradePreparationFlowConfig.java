package com.sequenceiq.datalake.flow.datalake.upgrade.preparation;

import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationEvent.DATALAKE_UPGRADE_PREPARATION_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationEvent.DATALAKE_UPGRADE_PREPARATION_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationEvent.DATALAKE_UPGRADE_PREPARATION_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationEvent.DATALAKE_UPGRADE_PREPARATION_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationEvent.DATALAKE_UPGRADE_PREPARATION_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationEvent.DATALAKE_UPGRADE_PREPARATION_TRIGGER_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationState.DATALAKE_UPGRADE_PREPARATION_FAILED_STATE;
import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationState.DATALAKE_UPGRADE_PREPARATION_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationState.DATALAKE_UPGRADE_PREPARATION_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationState.DATALAKE_UPGRADE_PREPARATION_START_STATE;
import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class DatalakeUpgradePreparationFlowConfig extends AbstractFlowConfiguration<DatalakeUpgradePreparationState, DatalakeUpgradePreparationEvent>
        implements RetryableDatalakeFlowConfiguration<DatalakeUpgradePreparationEvent> {

    private static final List<Transition<DatalakeUpgradePreparationState, DatalakeUpgradePreparationEvent>> TRANSITIONS =
            new Transition.Builder<DatalakeUpgradePreparationState, DatalakeUpgradePreparationEvent>()
                    .defaultFailureEvent(DATALAKE_UPGRADE_PREPARATION_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(DATALAKE_UPGRADE_PREPARATION_START_STATE)
                    .event(DATALAKE_UPGRADE_PREPARATION_TRIGGER_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_UPGRADE_PREPARATION_START_STATE)
                    .to(DATALAKE_UPGRADE_PREPARATION_IN_PROGRESS_STATE)
                    .event(DATALAKE_UPGRADE_PREPARATION_IN_PROGRESS_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_UPGRADE_PREPARATION_IN_PROGRESS_STATE)
                    .to(DATALAKE_UPGRADE_PREPARATION_FINISHED_STATE)
                    .event(DATALAKE_UPGRADE_PREPARATION_SUCCESS_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_UPGRADE_PREPARATION_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_UPGRADE_PREPARATION_FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<DatalakeUpgradePreparationState, DatalakeUpgradePreparationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATALAKE_UPGRADE_PREPARATION_FAILED_STATE, DATALAKE_UPGRADE_PREPARATION_FAILED_HANDLED_EVENT);

    protected DatalakeUpgradePreparationFlowConfig() {
        super(DatalakeUpgradePreparationState.class, DatalakeUpgradePreparationEvent.class);
    }

    @Override
    protected List<Transition<DatalakeUpgradePreparationState, DatalakeUpgradePreparationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatalakeUpgradePreparationState, DatalakeUpgradePreparationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DatalakeUpgradePreparationEvent[] getEvents() {
        return DatalakeUpgradePreparationEvent.values();
    }

    @Override
    public DatalakeUpgradePreparationEvent[] getInitEvents() {
        return new DatalakeUpgradePreparationEvent[]{DATALAKE_UPGRADE_PREPARATION_TRIGGER_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Prepare upgrade for Data Lake";
    }

    @Override
    public DatalakeUpgradePreparationEvent getRetryableEvent() {
        return DATALAKE_UPGRADE_PREPARATION_FAILED_HANDLED_EVENT;
    }

    @Override
    public List<DatalakeUpgradePreparationEvent> getStackRetryEvents() {
        return List.of(DATALAKE_UPGRADE_PREPARATION_IN_PROGRESS_EVENT);
    }
}
