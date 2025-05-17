package com.sequenceiq.cloudbreak.cloud.gcp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

class GcpInstanceStatusMapperTest {

    static Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of("RUNNING", InstanceStatus.STARTED),
                Arguments.of("TERMINATED", InstanceStatus.STOPPED),
                Arguments.of("UNKNOWN", InstanceStatus.IN_PROGRESS),
                Arguments.of(null, InstanceStatus.IN_PROGRESS),
                Arguments.of("", InstanceStatus.IN_PROGRESS),
                Arguments.of("running", InstanceStatus.STARTED),
                Arguments.of("terminated", InstanceStatus.STOPPED)
        );
    }

    @MethodSource("testCases")
    @ParameterizedTest
    void testGetInstanceStatusFromGcpStatus(String gcpStatus, InstanceStatus expected) {
        assertEquals(expected, GcpInstanceStatusMapper.getInstanceStatusFromGcpStatus(gcpStatus));
    }
}
