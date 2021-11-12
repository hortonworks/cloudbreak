package com.sequenceiq.environment.environment.flow.upgrade.ccm.config;

import static com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_DATAHUB_STATE;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_DATALAKE_STATE;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_FREEIPA_STATE;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_VALIDATION_STATE;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.FAILED_UPGRADE_CCM_EVENT;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.FINALIZE_UPGRADE_CCM_EVENT;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.FINISH_UPGRADE_CCM_EVENT;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.HANDLED_FAILED_UPGRADE_CCM_EVENT;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.UPGRADE_CCM_DATAHUB_EVENT;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.UPGRADE_CCM_DATALAKE_EVENT;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.UPGRADE_CCM_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.UPGRADE_CCM_VALIDATION_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class UpgradeCcmFlowConfig extends AbstractFlowConfiguration<UpgradeCcmState, UpgradeCcmStateSelectors>
        implements RetryableFlowConfiguration<UpgradeCcmStateSelectors> {

    private static final List<Transition<UpgradeCcmState, UpgradeCcmStateSelectors>> TRANSITIONS =
            new Transition.Builder<UpgradeCcmState, UpgradeCcmStateSelectors>()
            .defaultFailureEvent(FAILED_UPGRADE_CCM_EVENT)

            .from(INIT_STATE).to(UPGRADE_CCM_VALIDATION_STATE)
            .event(UPGRADE_CCM_VALIDATION_EVENT).defaultFailureEvent()

            .from(UPGRADE_CCM_VALIDATION_STATE).to(UPGRADE_CCM_FREEIPA_STATE)
            .event(UPGRADE_CCM_FREEIPA_EVENT).defaultFailureEvent()

            .from(UPGRADE_CCM_FREEIPA_STATE).to(UPGRADE_CCM_DATALAKE_STATE)
            .event(UPGRADE_CCM_DATALAKE_EVENT).defaultFailureEvent()

            .from(UPGRADE_CCM_DATALAKE_STATE).to(UPGRADE_CCM_DATAHUB_STATE)
            .event(UPGRADE_CCM_DATAHUB_EVENT).defaultFailureEvent()

            .from(UPGRADE_CCM_DATAHUB_STATE).to(UPGRADE_CCM_FINISHED_STATE)
            .event(FINISH_UPGRADE_CCM_EVENT).defaultFailureEvent()

            .from(UPGRADE_CCM_FINISHED_STATE).to(FINAL_STATE)
            .event(FINALIZE_UPGRADE_CCM_EVENT).defaultFailureEvent()

            .build();

    protected UpgradeCcmFlowConfig() {
        super(UpgradeCcmState.class, UpgradeCcmStateSelectors.class);
    }

    @Override
    protected List<Transition<UpgradeCcmState, UpgradeCcmStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<UpgradeCcmState, UpgradeCcmStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, UPGRADE_CCM_FAILED_STATE, HANDLED_FAILED_UPGRADE_CCM_EVENT);
    }

    @Override
    public UpgradeCcmStateSelectors[] getEvents() {
        return UpgradeCcmStateSelectors.values();
    }

    @Override
    public UpgradeCcmStateSelectors[] getInitEvents() {
        return new UpgradeCcmStateSelectors[] { UPGRADE_CCM_VALIDATION_EVENT };
    }

    @Override
    public String getDisplayName() {
        return "Upgrade CCM";
    }

    @Override
    public UpgradeCcmStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_UPGRADE_CCM_EVENT;
    }
}
