package com.sequenceiq.datalake.flow.upgrade.ccm;

import static com.sequenceiq.datalake.flow.upgrade.ccm.UpgradeCcmState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.upgrade.ccm.UpgradeCcmState.INIT_STATE;
import static com.sequenceiq.datalake.flow.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_FAILED_STATE;
import static com.sequenceiq.datalake.flow.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_UPGRADE_STACK_STATE;
import static com.sequenceiq.datalake.flow.upgrade.ccm.UpgradeCcmStateSelectors.UPGRADE_CCM_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.ccm.UpgradeCcmStateSelectors.UPGRADE_CCM_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.ccm.UpgradeCcmStateSelectors.UPGRADE_CCM_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.ccm.UpgradeCcmStateSelectors.UPGRADE_CCM_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.ccm.UpgradeCcmStateSelectors.UPGRADE_CCM_UPGRADE_STACK_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class UpgradeCcmFlowConfig extends AbstractFlowConfiguration<UpgradeCcmState, UpgradeCcmStateSelectors>
        implements RetryableDatalakeFlowConfiguration<UpgradeCcmStateSelectors> {

    private static final List<Transition<UpgradeCcmState, UpgradeCcmStateSelectors>> TRANSITIONS =
            new Transition.Builder<UpgradeCcmState, UpgradeCcmStateSelectors>()
            .defaultFailureEvent(UPGRADE_CCM_FAILED_EVENT)

            .from(INIT_STATE).to(UPGRADE_CCM_UPGRADE_STACK_STATE)
            .event(UPGRADE_CCM_UPGRADE_STACK_EVENT).defaultFailureEvent()

            .from(UPGRADE_CCM_UPGRADE_STACK_STATE).to(UPGRADE_CCM_FINISHED_STATE)
            .event(UPGRADE_CCM_SUCCESS_EVENT).defaultFailureEvent()

            .from(UPGRADE_CCM_FINISHED_STATE).to(FINAL_STATE)
            .event(UPGRADE_CCM_FINALIZED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<UpgradeCcmState, UpgradeCcmStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, UPGRADE_CCM_FAILED_STATE, UPGRADE_CCM_FAILED_HANDLED_EVENT);

    public UpgradeCcmFlowConfig() {
        super(UpgradeCcmState.class, UpgradeCcmStateSelectors.class);
    }

    @Override
    public UpgradeCcmStateSelectors[] getEvents() {
        return UpgradeCcmStateSelectors.values();
    }

    @Override
    public UpgradeCcmStateSelectors[] getInitEvents() {
        return new UpgradeCcmStateSelectors[]{
                UPGRADE_CCM_UPGRADE_STACK_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Upgrade CCM";
    }

    @Override
    protected List<Transition<UpgradeCcmState, UpgradeCcmStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<UpgradeCcmState, UpgradeCcmStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public UpgradeCcmStateSelectors getRetryableEvent() {
        return UPGRADE_CCM_FAILED_HANDLED_EVENT;
    }
}
