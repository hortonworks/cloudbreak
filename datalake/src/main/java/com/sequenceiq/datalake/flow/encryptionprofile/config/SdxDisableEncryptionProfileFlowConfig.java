package com.sequenceiq.datalake.flow.encryptionprofile.config;

import static com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileEvent.SDX_DISABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileEvent.SDX_DISABLE_ENCRYPTION_PROFILE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileEvent.SDX_DISABLE_ENCRYPTION_PROFILE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileEvent.SDX_DISABLE_ENCRYPTION_PROFILE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileEvent.SDX_DISABLE_ENCRYPTION_PROFILE_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileState.INIT_STATE;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileState.SDX_DISABLE_ENCRYPTION_PROFILE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileState.SDX_DISABLE_ENCRYPTION_PROFILE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileState.SDX_DISABLE_ENCRYPTION_PROFILE_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class SdxDisableEncryptionProfileFlowConfig
        extends AbstractFlowConfiguration<SdxDisableEncryptionProfileState, SdxDisableEncryptionProfileEvent>
        implements RetryableDatalakeFlowConfiguration<SdxDisableEncryptionProfileEvent> {

    private static final List<Transition<SdxDisableEncryptionProfileState, SdxDisableEncryptionProfileEvent>> TRANSITIONS =
            new Transition.Builder<SdxDisableEncryptionProfileState, SdxDisableEncryptionProfileEvent>()
                    .defaultFailureEvent(SDX_DISABLE_ENCRYPTION_PROFILE_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(SDX_DISABLE_ENCRYPTION_PROFILE_STATE)
                    .event(SDX_DISABLE_ENCRYPTION_PROFILE_EVENT)
                    .defaultFailureEvent()

                    .from(SDX_DISABLE_ENCRYPTION_PROFILE_STATE)
                    .to(SDX_DISABLE_ENCRYPTION_PROFILE_FINISHED_STATE)
                    .event(SDX_DISABLE_ENCRYPTION_PROFILE_SUCCESS_EVENT)
                    .defaultFailureEvent()

                    .from(SDX_DISABLE_ENCRYPTION_PROFILE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(SDX_DISABLE_ENCRYPTION_PROFILE_FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    protected SdxDisableEncryptionProfileFlowConfig() {
        super(SdxDisableEncryptionProfileState.class, SdxDisableEncryptionProfileEvent.class);
    }

    @Override
    protected List<Transition<SdxDisableEncryptionProfileState, SdxDisableEncryptionProfileEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<SdxDisableEncryptionProfileState, SdxDisableEncryptionProfileEvent> getEdgeConfig() {
        return new FlowEdgeConfig<>(
                INIT_STATE,
                FINAL_STATE,
                SDX_DISABLE_ENCRYPTION_PROFILE_FAILED_STATE,
                SDX_DISABLE_ENCRYPTION_PROFILE_FAIL_HANDLED_EVENT);
    }

    @Override
    public SdxDisableEncryptionProfileEvent[] getEvents() {
        return SdxDisableEncryptionProfileEvent.values();
    }

    @Override
    public SdxDisableEncryptionProfileEvent[] getInitEvents() {
        return new SdxDisableEncryptionProfileEvent[]{SDX_DISABLE_ENCRYPTION_PROFILE_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Disable encryption profile on Data Lake";
    }

    @Override
    public SdxDisableEncryptionProfileEvent getRetryableEvent() {
        return SDX_DISABLE_ENCRYPTION_PROFILE_FAIL_HANDLED_EVENT;
    }
}
