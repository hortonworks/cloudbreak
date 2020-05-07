package com.sequenceiq.datalake.flow.datalake.upgrade;

import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_IMAGE_CHANGE_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_COULD_NOT_START_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeState.DATALAKE_IMAGE_CHANGE_STATE;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeState.DATALAKE_UPGRADE_COULD_NOT_START_STATE;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeState.DATALAKE_UPGRADE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeState.DATALAKE_UPGRADE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeState.DATALAKE_UPGRADE_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeState.DATALAKE_UPGRADE_START_STATE;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class DatalakeUpgradeFlowConfig extends AbstractFlowConfiguration<DatalakeUpgradeState, DatalakeUpgradeEvent>
        implements RetryableFlowConfiguration<DatalakeUpgradeEvent> {

    private static final List<Transition<DatalakeUpgradeState, DatalakeUpgradeEvent>> TRANSITIONS =
            new Transition.Builder<DatalakeUpgradeState, DatalakeUpgradeEvent>()
                    .defaultFailureEvent(DATALAKE_UPGRADE_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(DATALAKE_UPGRADE_START_STATE)
                    .event(DATALAKE_UPGRADE_EVENT).noFailureEvent()

                    .from(DATALAKE_UPGRADE_START_STATE)
                    .to(DATALAKE_UPGRADE_IN_PROGRESS_STATE)
                    .event(DATALAKE_UPGRADE_IN_PROGRESS_EVENT)
                    .failureState(DATALAKE_UPGRADE_COULD_NOT_START_STATE)
                    .failureEvent(DATALAKE_UPGRADE_COULD_NOT_START_EVENT)

                    .from(DATALAKE_UPGRADE_IN_PROGRESS_STATE)
                    .to(DATALAKE_IMAGE_CHANGE_STATE)
                    .event(DATALAKE_IMAGE_CHANGE_EVENT).defaultFailureEvent()

                    .from(DATALAKE_IMAGE_CHANGE_STATE)
                    .to(DATALAKE_UPGRADE_FINISHED_STATE)
                    .event(DATALAKE_UPGRADE_SUCCESS_EVENT).defaultFailureEvent()

                    .from(DATALAKE_UPGRADE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_UPGRADE_FINALIZED_EVENT).defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<DatalakeUpgradeState, DatalakeUpgradeEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATALAKE_UPGRADE_FAILED_STATE, DATALAKE_UPGRADE_FAILED_HANDLED_EVENT);

    public DatalakeUpgradeFlowConfig() {
        super(DatalakeUpgradeState.class, DatalakeUpgradeEvent.class);
    }

    @Override
    public DatalakeUpgradeEvent[] getEvents() {
        return DatalakeUpgradeEvent.values();
    }

    @Override
    public DatalakeUpgradeEvent[] getInitEvents() {
        return new DatalakeUpgradeEvent[]{
                DATALAKE_UPGRADE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Upgrade SDX";
    }

    @Override
    protected List<Transition<DatalakeUpgradeState, DatalakeUpgradeEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<DatalakeUpgradeState, DatalakeUpgradeEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DatalakeUpgradeEvent getRetryableEvent() {
        return DATALAKE_UPGRADE_FAILED_HANDLED_EVENT;
    }
}
