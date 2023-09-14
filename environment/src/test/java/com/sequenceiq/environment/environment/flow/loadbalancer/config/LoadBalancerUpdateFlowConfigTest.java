package com.sequenceiq.environment.environment.flow.loadbalancer.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.environment.environment.flow.loadbalancer.LoadBalancerUpdateState;

class LoadBalancerUpdateFlowConfigTest {

    private final LoadBalancerUpdateFlowConfig underTest = new LoadBalancerUpdateFlowConfig();

    @ParameterizedTest(name = "Test environment usage status use case mapping with flow state: {0}")
    @EnumSource(LoadBalancerUpdateState.class)
    void testGetUseCaseForFlowState(LoadBalancerUpdateState flowState) {
        UsageProto.CDPEnvironmentStatus.Value actual = underTest.getUseCaseForFlowState(flowState);

        if (LoadBalancerUpdateState.INIT_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.LOAD_BALANCER_UPDATE_STARTED, actual);
        } else if (LoadBalancerUpdateState.LOAD_BALANCER_UPDATE_FINISHED_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.LOAD_BALANCER_UPDATE_FINISHED, actual);
        } else if (LoadBalancerUpdateState.LOAD_BALANCER_UPDATE_FAILED_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.LOAD_BALANCER_UPDATE_FAILED, actual);
        } else {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET, actual);
        }
    }
}