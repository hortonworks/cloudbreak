package com.sequenceiq.cloudbreak.grpc.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.grpc.Status;

class GrpcUtilTest {

    @ParameterizedTest
    @EnumSource(value = Status.Code.class, names = {"UNAVAILABLE", "NOT_FOUND"})
    void isRetryableShouldReturnTrueForTransientStatusCodes(Status.Code statusCode) {
        assertTrue(GrpcUtil.isRetryable(statusCode));
    }

    @ParameterizedTest
    @EnumSource(value = Status.Code.class, names = {"UNAVAILABLE", "NOT_FOUND"}, mode = EnumSource.Mode.EXCLUDE)
    void isRetryableShouldReturnFalseForNonTransientStatusCodes(Status.Code statusCode) {
        assertFalse(GrpcUtil.isRetryable(statusCode));
    }
}
