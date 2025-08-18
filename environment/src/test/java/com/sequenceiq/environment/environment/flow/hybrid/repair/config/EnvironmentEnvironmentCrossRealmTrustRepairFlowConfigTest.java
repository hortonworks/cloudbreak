package com.sequenceiq.environment.environment.flow.hybrid.repair.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.environment.environment.flow.hybrid.repair.EnvironmentCrossRealmTrustRepairState;

class EnvironmentEnvironmentCrossRealmTrustRepairFlowConfigTest {

    private final EnvironmentCrossRealmTrustRepairFlowConfig underTest = new EnvironmentCrossRealmTrustRepairFlowConfig();

    @ParameterizedTest(name = "Test environment usage status use case mapping with flow state: {0}")
    @EnumSource(EnvironmentCrossRealmTrustRepairState.class)
    void testGetUseCaseForFlowState(EnvironmentCrossRealmTrustRepairState flowState) {
        UsageProto.CDPEnvironmentStatus.Value actual = underTest.getUseCaseForFlowState(flowState);

    }
}