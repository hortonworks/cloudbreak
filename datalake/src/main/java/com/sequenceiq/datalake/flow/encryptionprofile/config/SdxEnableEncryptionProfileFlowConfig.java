package com.sequenceiq.datalake.flow.encryptionprofile.config;

import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileEvent.SDX_ENABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileEvent.SDX_ENABLE_ENCRYPTION_PROFILE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileEvent.SDX_ENABLE_ENCRYPTION_PROFILE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileEvent.SDX_ENABLE_ENCRYPTION_PROFILE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileEvent.SDX_ENABLE_ENCRYPTION_PROFILE_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileState.INIT_STATE;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileState.SDX_ENABLE_ENCRYPTION_PROFILE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileState.SDX_ENABLE_ENCRYPTION_PROFILE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileState.SDX_ENABLE_ENCRYPTION_PROFILE_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class SdxEnableEncryptionProfileFlowConfig
        extends AbstractFlowConfiguration<SdxEnableEncryptionProfileState, SdxEnableEncryptionProfileEvent>
        implements RetryableDatalakeFlowConfiguration<SdxEnableEncryptionProfileEvent> {

    private static final List<Transition<SdxEnableEncryptionProfileState, SdxEnableEncryptionProfileEvent>> TRANSITIONS =
            new Transition.Builder<SdxEnableEncryptionProfileState, SdxEnableEncryptionProfileEvent>()
                    .defaultFailureEvent(SDX_ENABLE_ENCRYPTION_PROFILE_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(SDX_ENABLE_ENCRYPTION_PROFILE_STATE)
                    .event(SDX_ENABLE_ENCRYPTION_PROFILE_EVENT)
                    .defaultFailureEvent()

                    .from(SDX_ENABLE_ENCRYPTION_PROFILE_STATE)
                    .to(SDX_ENABLE_ENCRYPTION_PROFILE_FINISHED_STATE)
                    .event(SDX_ENABLE_ENCRYPTION_PROFILE_SUCCESS_EVENT)
                    .defaultFailureEvent()

                    .from(SDX_ENABLE_ENCRYPTION_PROFILE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(SDX_ENABLE_ENCRYPTION_PROFILE_FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    protected SdxEnableEncryptionProfileFlowConfig() {
        super(SdxEnableEncryptionProfileState.class, SdxEnableEncryptionProfileEvent.class);
    }

    @Override
    protected List<Transition<SdxEnableEncryptionProfileState, SdxEnableEncryptionProfileEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<SdxEnableEncryptionProfileState, SdxEnableEncryptionProfileEvent> getEdgeConfig() {
        return new FlowEdgeConfig<>(
                INIT_STATE,
                FINAL_STATE,
                SDX_ENABLE_ENCRYPTION_PROFILE_FAILED_STATE,
                SDX_ENABLE_ENCRYPTION_PROFILE_FAIL_HANDLED_EVENT);
    }

    @Override
    public SdxEnableEncryptionProfileEvent[] getEvents() {
        return SdxEnableEncryptionProfileEvent.values();
    }

    @Override
    public SdxEnableEncryptionProfileEvent[] getInitEvents() {
        return new SdxEnableEncryptionProfileEvent[]{SDX_ENABLE_ENCRYPTION_PROFILE_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Enable encryption profile on datalake";
    }

    @Override
    public SdxEnableEncryptionProfileEvent getRetryableEvent() {
        return SDX_ENABLE_ENCRYPTION_PROFILE_FAIL_HANDLED_EVENT;
    }
}
