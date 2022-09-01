package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;

class RotateSaltPasswordReasonTest {

    @ParameterizedTest(name = "{0} has a corresponding usage event reason")
    @EnumSource(RotateSaltPasswordReason.class)
    void checkUsageValues(RotateSaltPasswordReason reason) {
        UsageProto.CDPSaltPasswordRotationEventReason.Value.valueOf(reason.name());
    }

    @ParameterizedTest
    @MethodSource("getForStatusArguments")
    void getForStatus(SaltPasswordStatus status, RotateSaltPasswordReason reason) {
        assertEquals(Optional.ofNullable(reason), RotateSaltPasswordReason.getForStatus(status));
    }

    private static Stream<Arguments> getForStatusArguments() {
        return Stream.of(
                Arguments.of(SaltPasswordStatus.OK, null),
                Arguments.of(SaltPasswordStatus.FAILED_TO_CHECK, null),
                Arguments.of(SaltPasswordStatus.EXPIRES, RotateSaltPasswordReason.EXPIRED),
                Arguments.of(SaltPasswordStatus.INVALID, RotateSaltPasswordReason.UNAUTHORIZED)
        );
    }
}
