package com.sequenceiq.environment.environment.flow.config.update.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.environment.environment.flow.config.update.EnvStackConfigUpdatesState;

class EnvStackConfigUpdatesFlowConfigTest {

    private final EnvStackConfigUpdatesFlowConfig underTest = new EnvStackConfigUpdatesFlowConfig();

    @ParameterizedTest(name = "Test environment usage status use case mapping with flow state: {0}")
    @EnumSource(EnvStackConfigUpdatesState.class)
    void testGetUseCaseForFlowState(EnvStackConfigUpdatesState flowState) {
        UsageProto.CDPEnvironmentStatus.Value actual = underTest.getUseCaseForFlowState(flowState);

        if (EnvStackConfigUpdatesState.INIT_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.ENVIRONMENT_STACK_CONFIG_UPDATE_STARTED, actual);
        } else if (EnvStackConfigUpdatesState.STACK_CONFIG_UPDATES_FINISHED_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.ENVIRONMENT_STACK_CONFIG_UPDATE_FINISHED, actual);
        } else if (EnvStackConfigUpdatesState.STACK_CONFIG_UPDATES_FAILED_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.ENVIRONMENT_STACK_CONFIG_UPDATE_FAILED, actual);
        } else {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET, actual);
        }
    }
}