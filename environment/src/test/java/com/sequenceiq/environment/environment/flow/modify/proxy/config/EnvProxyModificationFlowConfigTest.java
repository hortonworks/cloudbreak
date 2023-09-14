package com.sequenceiq.environment.environment.flow.modify.proxy.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState;

class EnvProxyModificationFlowConfigTest {

    private final EnvProxyModificationFlowConfig underTest = new EnvProxyModificationFlowConfig();

    @ParameterizedTest(name = "Test environment usage status use case mapping with flow state: {0}")
    @EnumSource(EnvProxyModificationState.class)
    void testGetUseCaseForFlowState(EnvProxyModificationState flowState) {
        UsageProto.CDPEnvironmentStatus.Value actual = underTest.getUseCaseForFlowState(flowState);

        if (EnvProxyModificationState.INIT_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.PROXY_MODIFICATION_STARTED, actual);
        } else if (EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_FINISHED_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.PROXY_MODIFICATION_FINISHED, actual);
        } else if (EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_FAILED_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.PROXY_MODIFICATION_FAILED, actual);
        } else {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET, actual);
        }
    }
}