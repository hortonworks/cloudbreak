package com.sequenceiq.environment.environment.flow.encryptionprofile.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.environment.environment.flow.encryptionprofile.EnabledEncryptionProfileState;

class EnableEncryptionProfileFlowConfigTest {

    private final EnableEncryptionProfileFlowConfig underTest = new EnableEncryptionProfileFlowConfig();

    @ParameterizedTest(name = "Test environment usage status use case mapping with flow state: {0}")
    @EnumSource(EnabledEncryptionProfileState.class)
    void testGetUseCaseForFlowState(EnabledEncryptionProfileState flowState) {
        UsageProto.CDPEnvironmentStatus.Value actual = underTest.getUseCaseForFlowState(flowState);

        if (EnabledEncryptionProfileState.INIT_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.SUSPEND_STARTED, actual);
        } else if (EnabledEncryptionProfileState.ENABLE_ENCRYPTION_PROFILE_FINISHED_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.SUSPEND_FINISHED, actual);
        } else if (EnabledEncryptionProfileState.ENABLE_ENCRYPTION_PROFILE_FAILED_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.SUSPEND_FAILED, actual);
        } else {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET, actual);
        }
    }
}