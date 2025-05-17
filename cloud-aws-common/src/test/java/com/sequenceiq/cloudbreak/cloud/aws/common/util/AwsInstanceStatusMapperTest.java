package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.StateReason;

class AwsInstanceStatusMapperTest {

    static Stream<Arguments> testCasesWithStateReason() {
        return Stream.of(
                // Happy paths
                Arguments.of(InstanceState.builder().name("stopped").build(), null,
                        InstanceStatus.STOPPED),
                Arguments.of(InstanceState.builder().name("running").build(), null,
                        InstanceStatus.STARTED),
                Arguments.of(InstanceState.builder().name("shutting-down").build(), null,
                        InstanceStatus.SHUTTING_DOWN),
                Arguments.of(InstanceState.builder().name("terminated").build(), null,
                        InstanceStatus.TERMINATED),
                Arguments.of(InstanceState.builder().name("terminated").build(), StateReason.builder().code("Server.SpotInstanceTermination").build(),
                        InstanceStatus.TERMINATED_BY_PROVIDER),
                Arguments.of(InstanceState.builder().name("unknown").build(), null,
                        InstanceStatus.IN_PROGRESS),

                // With non-relevant state reason
                Arguments.of(InstanceState.builder().name("stopped").build(), StateReason.builder().code("Server.InternalError").build(),
                        InstanceStatus.STOPPED),
                Arguments.of(InstanceState.builder().name("running").build(), StateReason.builder().code("Server.InternalError").build(),
                        InstanceStatus.STARTED),
                Arguments.of(InstanceState.builder().name("shutting-down").build(), StateReason.builder().code("Server.InternalError").build(),
                        InstanceStatus.SHUTTING_DOWN),
                Arguments.of(InstanceState.builder().name("terminated").build(), StateReason.builder().code("Server.InternalError").build(),
                        InstanceStatus.TERMINATED),
                Arguments.of(InstanceState.builder().name("unknown").build(), StateReason.builder().code("Server.InternalError").build(),
                        InstanceStatus.IN_PROGRESS)
        );
    }

    @MethodSource("testCasesWithStateReason")
    @ParameterizedTest
    void testGetInstanceStatusByAwsStateAndReason(InstanceState instanceState, StateReason stateReason, InstanceStatus expected) {
        assertEquals(expected, AwsInstanceStatusMapper.getInstanceStatusByAwsStateAndReason(instanceState, stateReason));
    }

    static Stream<Arguments> testCasesWithoutStateReason() {
        return Stream.of(
                Arguments.of("stopped", InstanceStatus.STOPPED),
                Arguments.of("running", InstanceStatus.STARTED),
                Arguments.of("shutting-down", InstanceStatus.SHUTTING_DOWN),
                Arguments.of("terminated", InstanceStatus.TERMINATED),
                Arguments.of("unknown", InstanceStatus.IN_PROGRESS),
                Arguments.of("STOPPED", InstanceStatus.STOPPED),
                Arguments.of("RUNNING", InstanceStatus.STARTED),
                Arguments.of("SHUTTING-DOWN", InstanceStatus.SHUTTING_DOWN),
                Arguments.of("TERMINATED", InstanceStatus.TERMINATED)
        );
    }

    @MethodSource("testCasesWithoutStateReason")
    @ParameterizedTest
    void testGetInstanceStatusByAwsStatus(String instanceStateString, InstanceStatus expected) {
        assertEquals(expected, AwsInstanceStatusMapper.getInstanceStatusByAwsStatus(instanceStateString));
    }

}
