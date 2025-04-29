package com.sequenceiq.freeipa.flow.freeipa.enableselinux.config;

import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.FreeIpaModifySeLinuxState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.FreeIpaModifySeLinuxState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.FreeIpaModifySeLinuxState.MODIFY_SELINUX_FREEIPA_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.FreeIpaModifySeLinuxState.MODIFY_SELINUX_FREEIPA_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.FreeIpaModifySeLinuxState.MODIFY_SELINUX_FREEIPA_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.FreeIpaModifySeLinuxState.MODIFY_SELINUX_FREEIPA_VALIDATION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors.FAILED_MODIFY_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors.FINALIZE_MODIFY_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors.FINISH_MODIFY_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors.HANDLED_FAILED_MODIFY_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors.MODIFY_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors.MODIFY_SELINUX_START_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.FreeIpaModifySeLinuxState;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors;

@Component
public class FreeIpaModifySeLinuxFlowConfig extends AbstractFlowConfiguration<FreeIpaModifySeLinuxState, FreeIpaModifySeLinuxStateSelectors>
        implements RetryableFlowConfiguration<FreeIpaModifySeLinuxStateSelectors>, FreeIpaUseCaseAware {

    private static final List<Transition<FreeIpaModifySeLinuxState, FreeIpaModifySeLinuxStateSelectors>> TRANSITIONS =
            new Transition.Builder<FreeIpaModifySeLinuxState, FreeIpaModifySeLinuxStateSelectors>()
            .defaultFailureEvent(FAILED_MODIFY_SELINUX_FREEIPA_EVENT)

            .from(FreeIpaModifySeLinuxState.INIT_STATE)
            .to(MODIFY_SELINUX_FREEIPA_VALIDATION_STATE)
            .event(MODIFY_SELINUX_START_EVENT)
            .defaultFailureEvent()

            .from(MODIFY_SELINUX_FREEIPA_VALIDATION_STATE)
            .to(MODIFY_SELINUX_FREEIPA_STATE)
            .event(MODIFY_SELINUX_FREEIPA_EVENT)
            .defaultFailureEvent()

            .from(MODIFY_SELINUX_FREEIPA_STATE)
            .to(MODIFY_SELINUX_FREEIPA_FINISHED_STATE)
            .event(FINISH_MODIFY_SELINUX_FREEIPA_EVENT)
            .defaultFailureEvent()

            .from(MODIFY_SELINUX_FREEIPA_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(FINALIZE_MODIFY_SELINUX_FREEIPA_EVENT)
            .defaultFailureEvent()

            .build();

    protected FreeIpaModifySeLinuxFlowConfig() {
        super(FreeIpaModifySeLinuxState.class, FreeIpaModifySeLinuxStateSelectors.class);
    }

    @Override
    protected List<Transition<FreeIpaModifySeLinuxState, FreeIpaModifySeLinuxStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<FreeIpaModifySeLinuxState, FreeIpaModifySeLinuxStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, MODIFY_SELINUX_FREEIPA_FAILED_STATE, HANDLED_FAILED_MODIFY_SELINUX_FREEIPA_EVENT);
    }

    @Override
    public FreeIpaModifySeLinuxStateSelectors[] getEvents() {
        return FreeIpaModifySeLinuxStateSelectors.values();
    }

    @Override
    public FreeIpaModifySeLinuxStateSelectors[] getInitEvents() {
        return new FreeIpaModifySeLinuxStateSelectors[] {MODIFY_SELINUX_START_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Enable SeLinux FreeIPA";
    }

    @Override
    public FreeIpaModifySeLinuxStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_MODIFY_SELINUX_FREEIPA_EVENT;
    }

    @Override
    public UsageProto.CDPFreeIPAStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return UsageProto.CDPFreeIPAStatus.Value.ENABLE_SELINUX_STARTED;
        } else if (MODIFY_SELINUX_FREEIPA_FAILED_STATE.equals(flowState)) {
            return UsageProto.CDPFreeIPAStatus.Value.ENABLE_SELINUX_FAILED;
        } else if (MODIFY_SELINUX_FREEIPA_FINISHED_STATE.equals(flowState)) {
            return UsageProto.CDPFreeIPAStatus.Value.ENABLE_SELINUX_FINISHED;
        }
        return UsageProto.CDPFreeIPAStatus.Value.UNSET;
    }
}
