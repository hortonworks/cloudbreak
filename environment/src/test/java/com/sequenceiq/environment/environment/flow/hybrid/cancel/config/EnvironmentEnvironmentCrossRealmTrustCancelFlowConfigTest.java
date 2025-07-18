package com.sequenceiq.environment.environment.flow.hybrid.cancel.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState;

class EnvironmentEnvironmentCrossRealmTrustCancelFlowConfigTest {

    private final EnvironmentCrossRealmTrustCancelFlowConfig underTest = new EnvironmentCrossRealmTrustCancelFlowConfig();

    @ParameterizedTest(name = "Test environment usage status use case mapping with flow state: {0}")
    @EnumSource(EnvironmentCrossRealmTrustCancelState.class)
    void testGetUseCaseForFlowState(EnvironmentCrossRealmTrustCancelState flowState) {
        UsageProto.CDPEnvironmentStatus.Value actual = underTest.getUseCaseForFlowState(flowState);

    }
}