package com.sequenceiq.environment.environment.flow.hybrid.setup.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState;

class EnvironmentEnvironmentCrossRealmTrustSetupFlowConfigTest {

    private final EnvironmentCrossRealmTrustSetupFlowConfig underTest = new EnvironmentCrossRealmTrustSetupFlowConfig();

    @ParameterizedTest(name = "Test environment usage status use case mapping with flow state: {0}")
    @EnumSource(EnvironmentCrossRealmTrustSetupState.class)
    void testGetUseCaseForFlowState(EnvironmentCrossRealmTrustSetupState flowState) {
        UsageProto.CDPEnvironmentStatus.Value actual = underTest.getUseCaseForFlowState(flowState);

    }
}