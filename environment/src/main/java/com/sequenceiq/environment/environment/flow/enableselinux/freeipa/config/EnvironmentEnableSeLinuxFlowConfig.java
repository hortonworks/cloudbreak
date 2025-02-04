package com.sequenceiq.environment.environment.flow.enableselinux.freeipa.config;

import static com.sequenceiq.environment.environment.flow.enableselinux.freeipa.EnvironmentEnableSeLinuxState.ENABLE_SELINUX_FREEIPA_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.enableselinux.freeipa.EnvironmentEnableSeLinuxState.ENABLE_SELINUX_FREEIPA_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.enableselinux.freeipa.EnvironmentEnableSeLinuxState.ENABLE_SELINUX_FREEIPA_STATE;
import static com.sequenceiq.environment.environment.flow.enableselinux.freeipa.EnvironmentEnableSeLinuxState.ENABLE_SELINUX_FREEIPA_VALIDATION_STATE;
import static com.sequenceiq.environment.environment.flow.enableselinux.freeipa.EnvironmentEnableSeLinuxState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.enableselinux.freeipa.EnvironmentEnableSeLinuxState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxStateSelectors.ENABLE_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxStateSelectors.ENABLE_SELINUX_FREEIPA_VALIDATION_EVENT;
import static com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxStateSelectors.FAILED_ENABLE_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxStateSelectors.FINALIZE_ENABLE_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxStateSelectors.FINISH_ENABLE_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxStateSelectors.HANDLED_FAILED_ENABLE_SELINUX_FREEIPA_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseAware;
import com.sequenceiq.environment.environment.flow.enableselinux.freeipa.EnvironmentEnableSeLinuxState;
import com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxStateSelectors;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class EnvironmentEnableSeLinuxFlowConfig extends AbstractFlowConfiguration<EnvironmentEnableSeLinuxState, EnvironmentEnableSeLinuxStateSelectors>
        implements RetryableFlowConfiguration<EnvironmentEnableSeLinuxStateSelectors>, EnvironmentUseCaseAware {

    private static final List<Transition<EnvironmentEnableSeLinuxState, EnvironmentEnableSeLinuxStateSelectors>> TRANSITIONS =
            new Transition.Builder<EnvironmentEnableSeLinuxState, EnvironmentEnableSeLinuxStateSelectors>()
            .defaultFailureEvent(FAILED_ENABLE_SELINUX_FREEIPA_EVENT)

            .from(EnvironmentEnableSeLinuxState.INIT_STATE)
            .to(ENABLE_SELINUX_FREEIPA_VALIDATION_STATE)
            .event(ENABLE_SELINUX_FREEIPA_VALIDATION_EVENT)
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

    protected EnvironmentEnableSeLinuxFlowConfig() {
        super(EnvironmentEnableSeLinuxState.class, EnvironmentEnableSeLinuxStateSelectors.class);
    }

    @Override
    protected List<Transition<EnvironmentEnableSeLinuxState, EnvironmentEnableSeLinuxStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<EnvironmentEnableSeLinuxState, EnvironmentEnableSeLinuxStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, ENABLE_SELINUX_FREEIPA_FAILED_STATE, HANDLED_FAILED_ENABLE_SELINUX_FREEIPA_EVENT);
    }

    @Override
    public EnvironmentEnableSeLinuxStateSelectors[] getEvents() {
        return EnvironmentEnableSeLinuxStateSelectors.values();
    }

    @Override
    public EnvironmentEnableSeLinuxStateSelectors[] getInitEvents() {
        return new EnvironmentEnableSeLinuxStateSelectors[] {ENABLE_SELINUX_FREEIPA_VALIDATION_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Enable SeLinux FreeIPA";
    }

    @Override
    public EnvironmentEnableSeLinuxStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_ENABLE_SELINUX_FREEIPA_EVENT;
    }

    @Override
    public UsageProto.CDPEnvironmentStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
//        if (INIT_STATE.equals(flowState)) {
//            return UsageProto.CDPEnvironmentStatus.Value.ENVIRONMENT_ENABLE_SELINUX_STARTED;
//        } else if (ENABLE_SELINUX_FREEIPA_FAILED_STATE.equals(flowState)) {
//            return UsageProto.CDPEnvironmentStatus.Value.ENVIRONMENT_ENABLE_SELINUX_FAILED;
//        } else if (ENABLE_SELINUX_FREEIPA_FINISHED_STATE.equals(flowState)) {
//            return UsageProto.CDPEnvironmentStatus.Value.ENVIRONMENT_ENABLE_SELINUX_FINISHED;
//        }
        return UsageProto.CDPEnvironmentStatus.Value.UNSET;
    }
}
