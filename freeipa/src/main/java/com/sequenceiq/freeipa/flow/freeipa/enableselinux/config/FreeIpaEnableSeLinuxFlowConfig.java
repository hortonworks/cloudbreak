package com.sequenceiq.freeipa.flow.freeipa.enableselinux.config;

import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.FreeIpaEnableSeLinuxState.ENABLE_SELINUX_FREEIPA_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.FreeIpaEnableSeLinuxState.ENABLE_SELINUX_FREEIPA_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.FreeIpaEnableSeLinuxState.ENABLE_SELINUX_FREEIPA_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.FreeIpaEnableSeLinuxState.ENABLE_SELINUX_FREEIPA_VALIDATION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.FreeIpaEnableSeLinuxState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.FreeIpaEnableSeLinuxState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxStateSelectors.ENABLE_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxStateSelectors.FAILED_ENABLE_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxStateSelectors.FINALIZE_ENABLE_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxStateSelectors.FINISH_ENABLE_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxStateSelectors.HANDLED_FAILED_ENABLE_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxStateSelectors.SET_SELINUX_TO_ENFORCING_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.FreeIpaEnableSeLinuxState;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxStateSelectors;

@Component
public class FreeIpaEnableSeLinuxFlowConfig extends AbstractFlowConfiguration<FreeIpaEnableSeLinuxState, FreeIpaEnableSeLinuxStateSelectors>
        implements RetryableFlowConfiguration<FreeIpaEnableSeLinuxStateSelectors>, FreeIpaUseCaseAware {

    private static final List<Transition<FreeIpaEnableSeLinuxState, FreeIpaEnableSeLinuxStateSelectors>> TRANSITIONS =
            new Transition.Builder<FreeIpaEnableSeLinuxState, FreeIpaEnableSeLinuxStateSelectors>()
            .defaultFailureEvent(FAILED_ENABLE_SELINUX_FREEIPA_EVENT)

            .from(FreeIpaEnableSeLinuxState.INIT_STATE)
            .to(ENABLE_SELINUX_FREEIPA_VALIDATION_STATE)
            .event(SET_SELINUX_TO_ENFORCING_EVENT)
            .defaultFailureEvent()

            .from(ENABLE_SELINUX_FREEIPA_VALIDATION_STATE)
            .to(ENABLE_SELINUX_FREEIPA_STATE)
            .event(ENABLE_SELINUX_FREEIPA_EVENT)
            .defaultFailureEvent()

            .from(ENABLE_SELINUX_FREEIPA_STATE)
            .to(ENABLE_SELINUX_FREEIPA_FINISHED_STATE)
            .event(FINISH_ENABLE_SELINUX_FREEIPA_EVENT)
            .defaultFailureEvent()

            .from(ENABLE_SELINUX_FREEIPA_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(FINALIZE_ENABLE_SELINUX_FREEIPA_EVENT)
            .defaultFailureEvent()

            .build();

    protected FreeIpaEnableSeLinuxFlowConfig() {
        super(FreeIpaEnableSeLinuxState.class, FreeIpaEnableSeLinuxStateSelectors.class);
    }

    @Override
    protected List<Transition<FreeIpaEnableSeLinuxState, FreeIpaEnableSeLinuxStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<FreeIpaEnableSeLinuxState, FreeIpaEnableSeLinuxStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, ENABLE_SELINUX_FREEIPA_FAILED_STATE, HANDLED_FAILED_ENABLE_SELINUX_FREEIPA_EVENT);
    }

    @Override
    public FreeIpaEnableSeLinuxStateSelectors[] getEvents() {
        return FreeIpaEnableSeLinuxStateSelectors.values();
    }

    @Override
    public FreeIpaEnableSeLinuxStateSelectors[] getInitEvents() {
        return new FreeIpaEnableSeLinuxStateSelectors[] {SET_SELINUX_TO_ENFORCING_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Enable SeLinux FreeIPA";
    }

    @Override
    public FreeIpaEnableSeLinuxStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_ENABLE_SELINUX_FREEIPA_EVENT;
    }

    @Override
    public UsageProto.CDPFreeIPAStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return UsageProto.CDPFreeIPAStatus.Value.ENABLE_SELINUX_STARTED;
        } else if (ENABLE_SELINUX_FREEIPA_FAILED_STATE.equals(flowState)) {
            return UsageProto.CDPFreeIPAStatus.Value.ENABLE_SELINUX_FAILED;
        } else if (ENABLE_SELINUX_FREEIPA_FINISHED_STATE.equals(flowState)) {
            return UsageProto.CDPFreeIPAStatus.Value.ENABLE_SELINUX_FINISHED;
        }
        return UsageProto.CDPFreeIPAStatus.Value.UNSET;
    }
}
