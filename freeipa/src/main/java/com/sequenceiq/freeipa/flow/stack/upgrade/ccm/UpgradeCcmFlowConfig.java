package com.sequenceiq.freeipa.flow.stack.upgrade.ccm;

import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_CHECK_PREREQUISITES_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_CHECK_PREREQUISITES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_TRIGGER_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector;

@Component
public class UpgradeCcmFlowConfig extends AbstractFlowConfiguration<UpgradeCcmState, UpgradeCcmStateSelector>
        implements RetryableFlowConfiguration<UpgradeCcmStateSelector> {

    private static final UpgradeCcmStateSelector[] INIT_EVENTS = {UPGRADE_CCM_TRIGGER_EVENT};

    private static final FlowEdgeConfig<UpgradeCcmState, UpgradeCcmStateSelector> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, UPGRADE_CCM_FAILED_STATE, UPGRADE_CCM_FAILURE_HANDLED_EVENT);

    private static final List<Transition<UpgradeCcmState, UpgradeCcmStateSelector>> TRANSITIONS =
            new Transition.Builder<UpgradeCcmState, UpgradeCcmStateSelector>().defaultFailureEvent(UPGRADE_CCM_FAILED_EVENT)
                    .from(INIT_STATE).to(UPGRADE_CCM_CHECK_PREREQUISITES_STATE).event(UPGRADE_CCM_TRIGGER_EVENT).defaultFailureEvent()
                    .from(UPGRADE_CCM_CHECK_PREREQUISITES_STATE).to(UPGRADE_CCM_FINISHED_STATE).event(UPGRADE_CCM_CHECK_PREREQUISITES_FINISHED_EVENT)
                    .defaultFailureEvent()
                    .from(UPGRADE_CCM_FINISHED_STATE).to(FINAL_STATE).event(UPGRADE_CCM_FINISHED_EVENT).defaultFailureEvent()
                    .build();

    public UpgradeCcmFlowConfig() {
        super(UpgradeCcmState.class, UpgradeCcmStateSelector.class);
    }

    @Override
    public UpgradeCcmStateSelector[] getInitEvents() {
        return INIT_EVENTS;
    }

    @Override
    public UpgradeCcmStateSelector[] getEvents() {
        return UpgradeCcmStateSelector.values();
    }

    @Override
    public String getDisplayName() {
        return "Upgrade CCM";
    }

    @Override
    protected FlowEdgeConfig<UpgradeCcmState, UpgradeCcmStateSelector> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    protected List<Transition<UpgradeCcmState, UpgradeCcmStateSelector>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public UpgradeCcmStateSelector getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }

}
