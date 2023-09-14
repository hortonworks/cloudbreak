package com.sequenceiq.environment.environment.flow.verticalscale.freeipa.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.environment.environment.flow.verticalscale.freeipa.EnvironmentVerticalScaleState;

class EnvironmentVerticalScaleFlowConfigTest {

    private final EnvironmentVerticalScaleFlowConfig underTest = new EnvironmentVerticalScaleFlowConfig();

    @ParameterizedTest(name = "Test environment usage status use case mapping with flow state: {0}")
    @EnumSource(EnvironmentVerticalScaleState.class)
    void testGetUseCaseForFlowState(EnvironmentVerticalScaleState flowState) {
        UsageProto.CDPEnvironmentStatus.Value actual = underTest.getUseCaseForFlowState(flowState);

        if (EnvironmentVerticalScaleState.INIT_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.ENVIRONMENT_VERTICAL_SCALE_STARTED, actual);
        } else if (EnvironmentVerticalScaleState.VERTICAL_SCALING_FREEIPA_FINISHED_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.ENVIRONMENT_VERTICAL_SCALE_FINISHED, actual);
        } else if (EnvironmentVerticalScaleState.VERTICAL_SCALING_FREEIPA_FAILED_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.ENVIRONMENT_VERTICAL_SCALE_FAILED, actual);
        } else {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET, actual);
        }
    }
}