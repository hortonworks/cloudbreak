package com.sequenceiq.environment.environment.flow.encryptionprofile.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.environment.environment.flow.encryptionprofile.DisableEncryptionProfileState;

class DisableEncryptionProfileFlowConfigTest {

    private final DisableEncryptionProfileFlowConfig underTest = new DisableEncryptionProfileFlowConfig();

    @ParameterizedTest(name = "Test environment usage status use case mapping with flow state: {0}")
    @EnumSource(DisableEncryptionProfileState.class)
    void testGetUseCaseForFlowState(DisableEncryptionProfileState flowState) {
        UsageProto.CDPEnvironmentStatus.Value actual = underTest.getUseCaseForFlowState(flowState);

        if (DisableEncryptionProfileState.INIT_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.DISABLE_ENCRYPTION_PROFILE_STARTED, actual);
        } else if (DisableEncryptionProfileState.DISABLE_ENCRYPTION_PROFILE_FINISHED_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.DISABLE_ENCRYPTION_PROFILE_FINISHED, actual);
        } else if (DisableEncryptionProfileState.DISABLE_ENCRYPTION_PROFILE_FAILED_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.DISABLE_ENCRYPTION_PROFILE_FAILED, actual);
        } else {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET, actual);
        }
    }
}
