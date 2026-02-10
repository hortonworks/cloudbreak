package com.sequenceiq.environment.environment.flow.encryptionprofile.config;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.SUSPEND_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.SUSPEND_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.SUSPEND_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.UNSET;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.EnabledEncryptionProfileState.ENABLE_ENCRYPTION_PROFILE_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.EnabledEncryptionProfileState.ENABLE_ENCRYPTION_PROFILE_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.EnabledEncryptionProfileState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.EnabledEncryptionProfileState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.EnabledEncryptionProfileState.SET_ENCRYPTION_PROFILE_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.EnabledEncryptionProfileState.UPDATE_SSL_CONFIG_CLUSTERS_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.EnabledEncryptionProfileState.UPDATE_SSL_CONFIG_FREEIPA_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.EnabledEncryptionProfileState.VALIDATE_ENABLE_ENCRYPTION_PROFILE_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.FINALIZE_ENABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.HANDLED_FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.SET_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.UPDATE_SSL_CONFIG_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.UPDATE_SSL_CONFIG_IN_CLUSTERS_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.VALIDATE_ENABLE_ENCRYPTION_PROFILE_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseAware;
import com.sequenceiq.environment.environment.flow.encryptionprofile.EnabledEncryptionProfileState;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class EnableEncryptionProfileFlowConfig extends AbstractFlowConfiguration<EnabledEncryptionProfileState, EnableEncryptionProfileStateSelectors>
        implements RetryableFlowConfiguration<EnableEncryptionProfileStateSelectors>, EnvironmentUseCaseAware {

    private static final List<Transition<EnabledEncryptionProfileState, EnableEncryptionProfileStateSelectors>>
            TRANSITIONS = new Transition.Builder<EnabledEncryptionProfileState, EnableEncryptionProfileStateSelectors>()
            .defaultFailureEvent(FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT)

            .from(INIT_STATE).to(VALIDATE_ENABLE_ENCRYPTION_PROFILE_STATE)
            .event(VALIDATE_ENABLE_ENCRYPTION_PROFILE_EVENT).defaultFailureEvent()

            .from(VALIDATE_ENABLE_ENCRYPTION_PROFILE_STATE).to(SET_ENCRYPTION_PROFILE_STATE)
            .event(SET_ENCRYPTION_PROFILE_EVENT).defaultFailureEvent()

            .from(SET_ENCRYPTION_PROFILE_STATE).to(UPDATE_SSL_CONFIG_FREEIPA_STATE)
            .event(UPDATE_SSL_CONFIG_FREEIPA_EVENT).defaultFailureEvent()

            .from(UPDATE_SSL_CONFIG_FREEIPA_STATE).to(UPDATE_SSL_CONFIG_CLUSTERS_STATE)
            .event(UPDATE_SSL_CONFIG_IN_CLUSTERS_EVENT).defaultFailureEvent()

            .from(UPDATE_SSL_CONFIG_CLUSTERS_STATE).to(ENABLE_ENCRYPTION_PROFILE_FINISHED_STATE)
            .event(FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT).defaultFailureEvent()

            .from(ENABLE_ENCRYPTION_PROFILE_FINISHED_STATE).to(FINAL_STATE)
            .event(FINALIZE_ENABLE_ENCRYPTION_PROFILE_EVENT).defaultFailureEvent()

            .build();

    protected EnableEncryptionProfileFlowConfig() {
        super(EnabledEncryptionProfileState.class, EnableEncryptionProfileStateSelectors.class);
    }

    @Override
    protected List<Transition<EnabledEncryptionProfileState, EnableEncryptionProfileStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<EnabledEncryptionProfileState, EnableEncryptionProfileStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, ENABLE_ENCRYPTION_PROFILE_FAILED_STATE, HANDLED_FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT);
    }

    @Override
    public EnableEncryptionProfileStateSelectors[] getEvents() {
        return EnableEncryptionProfileStateSelectors.values();
    }

    @Override
    public EnableEncryptionProfileStateSelectors[] getInitEvents() {
        return new EnableEncryptionProfileStateSelectors[]{VALIDATE_ENABLE_ENCRYPTION_PROFILE_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Enable Encryption Profile";
    }

    @Override
    public EnableEncryptionProfileStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT;
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return SUSPEND_STARTED;
        } else if (ENABLE_ENCRYPTION_PROFILE_FINISHED_STATE.equals(flowState)) {
            return SUSPEND_FINISHED;
        } else if (ENABLE_ENCRYPTION_PROFILE_FAILED_STATE.equals(flowState)) {
            return SUSPEND_FAILED;
        } else {
            return UNSET;
        }
    }
}
