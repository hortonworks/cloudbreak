package com.sequenceiq.environment.environment.flow.encryptionprofile.config;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.DISABLE_ENCRYPTION_PROFILE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.DISABLE_ENCRYPTION_PROFILE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.DISABLE_ENCRYPTION_PROFILE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.UNSET;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.DisableEncryptionProfileState.DISABLE_ENCRYPTION_PROFILE_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.DisableEncryptionProfileState.DISABLE_ENCRYPTION_PROFILE_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.DisableEncryptionProfileState.DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.DisableEncryptionProfileState.DISABLE_ENCRYPTION_PROFILE_ON_STACKS_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.DisableEncryptionProfileState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.DisableEncryptionProfileState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.DisableEncryptionProfileState.UNSET_ENCRYPTION_PROFILE_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.DISABLE_ENCRYPTION_PROFILE_ON_STACKS_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.FINALIZE_DISABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.FINISH_DISABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.HANDLED_FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.UNSET_ENCRYPTION_PROFILE_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseAware;
import com.sequenceiq.environment.environment.flow.encryptionprofile.DisableEncryptionProfileState;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class DisableEncryptionProfileFlowConfig extends AbstractFlowConfiguration<DisableEncryptionProfileState, DisableEncryptionProfileStateSelectors>
        implements RetryableFlowConfiguration<DisableEncryptionProfileStateSelectors>, EnvironmentUseCaseAware {

    private static final List<Transition<DisableEncryptionProfileState, DisableEncryptionProfileStateSelectors>>
            TRANSITIONS = new Transition.Builder<DisableEncryptionProfileState, DisableEncryptionProfileStateSelectors>()
            .defaultFailureEvent(FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT)

            .from(INIT_STATE).to(UNSET_ENCRYPTION_PROFILE_STATE)
            .event(UNSET_ENCRYPTION_PROFILE_EVENT).defaultFailureEvent()

            .from(UNSET_ENCRYPTION_PROFILE_STATE).to(DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_STATE)
            .event(DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_EVENT).defaultFailureEvent()

            .from(DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_STATE).to(DISABLE_ENCRYPTION_PROFILE_ON_STACKS_STATE)
            .event(DISABLE_ENCRYPTION_PROFILE_ON_STACKS_EVENT).defaultFailureEvent()

            .from(DISABLE_ENCRYPTION_PROFILE_ON_STACKS_STATE).to(DISABLE_ENCRYPTION_PROFILE_FINISHED_STATE)
            .event(FINISH_DISABLE_ENCRYPTION_PROFILE_EVENT).defaultFailureEvent()

            .from(DISABLE_ENCRYPTION_PROFILE_FINISHED_STATE).to(FINAL_STATE)
            .event(FINALIZE_DISABLE_ENCRYPTION_PROFILE_EVENT).defaultFailureEvent()

            .build();

    protected DisableEncryptionProfileFlowConfig() {
        super(DisableEncryptionProfileState.class, DisableEncryptionProfileStateSelectors.class);
    }

    @Override
    protected List<Transition<DisableEncryptionProfileState, DisableEncryptionProfileStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DisableEncryptionProfileState, DisableEncryptionProfileStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DISABLE_ENCRYPTION_PROFILE_FAILED_STATE, HANDLED_FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT);
    }

    @Override
    public DisableEncryptionProfileStateSelectors[] getEvents() {
        return DisableEncryptionProfileStateSelectors.values();
    }

    @Override
    public DisableEncryptionProfileStateSelectors[] getInitEvents() {
        return new DisableEncryptionProfileStateSelectors[]{UNSET_ENCRYPTION_PROFILE_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Disable Encryption Profile";
    }

    @Override
    public DisableEncryptionProfileStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT;
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return DISABLE_ENCRYPTION_PROFILE_STARTED;
        } else if (DISABLE_ENCRYPTION_PROFILE_FINISHED_STATE.equals(flowState)) {
            return DISABLE_ENCRYPTION_PROFILE_FINISHED;
        } else if (DISABLE_ENCRYPTION_PROFILE_FAILED_STATE.equals(flowState)) {
            return DISABLE_ENCRYPTION_PROFILE_FAILED;
        } else {
            return UNSET;
        }
    }
}
