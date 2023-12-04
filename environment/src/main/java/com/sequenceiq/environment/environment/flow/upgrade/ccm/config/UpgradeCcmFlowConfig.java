package com.sequenceiq.environment.environment.flow.upgrade.ccm.config;

import static com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_DATAHUB_STATE;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_DATALAKE_STATE;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_FREEIPA_STATE;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_TUNNEL_UPDATE_STATE;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_VALIDATION_STATE;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.FAILED_UPGRADE_CCM_EVENT;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.FINALIZE_UPGRADE_CCM_EVENT;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.FINISH_UPGRADE_CCM_EVENT;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.HANDLED_FAILED_UPGRADE_CCM_EVENT;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.UPGRADE_CCM_DATAHUB_EVENT;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.UPGRADE_CCM_DATALAKE_EVENT;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.UPGRADE_CCM_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.UPGRADE_CCM_TUNNEL_UPDATE_EVENT;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.UPGRADE_CCM_VALIDATION_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseAware;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class UpgradeCcmFlowConfig extends AbstractFlowConfiguration<UpgradeCcmState, UpgradeCcmStateSelectors>
        implements RetryableFlowConfiguration<UpgradeCcmStateSelectors>, EnvironmentUseCaseAware {

    private static final List<Transition<UpgradeCcmState, UpgradeCcmStateSelectors>> TRANSITIONS =
            new Transition.Builder<UpgradeCcmState, UpgradeCcmStateSelectors>()
            .defaultFailureEvent(FAILED_UPGRADE_CCM_EVENT)

            .from(INIT_STATE).to(UPGRADE_CCM_VALIDATION_STATE)
            .event(UPGRADE_CCM_VALIDATION_EVENT).defaultFailureEvent()

            .from(UPGRADE_CCM_VALIDATION_STATE).to(UPGRADE_CCM_FREEIPA_STATE)
            .event(UPGRADE_CCM_FREEIPA_EVENT).defaultFailureEvent()

            .from(UPGRADE_CCM_FREEIPA_STATE).to(UPGRADE_CCM_TUNNEL_UPDATE_STATE)
            .event(UPGRADE_CCM_TUNNEL_UPDATE_EVENT).defaultFailureEvent()

            .from(UPGRADE_CCM_TUNNEL_UPDATE_STATE).to(UPGRADE_CCM_DATALAKE_STATE)
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
    public FlowEdgeConfig<UpgradeCcmState, UpgradeCcmStateSelectors> getEdgeConfig() {
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

    @Override
    public UsageProto.CDPEnvironmentStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return UsageProto.CDPEnvironmentStatus.Value.CCM_UPGRADE_STARTED;
        } else if (UPGRADE_CCM_FAILED_STATE.equals(flowState)) {
            return UsageProto.CDPEnvironmentStatus.Value.CCM_UPGRADE_FAILED;
        } else if (UPGRADE_CCM_FINISHED_STATE.equals(flowState)) {
            return UsageProto.CDPEnvironmentStatus.Value.CCM_UPGRADE_FINISHED;
        }
        return UsageProto.CDPEnvironmentStatus.Value.UNSET;
    }
}
