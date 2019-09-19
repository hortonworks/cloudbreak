package com.sequenceiq.cloudbreak.idbmms.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

class IdbmmsOperationErrorStatusTest {

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] fromThrowableDataProvider() {
        return new Object[][] {
                // testCaseName     code                    errorStatusExpected
                { "NOT_FOUND",      Status.Code.NOT_FOUND,  IdbmmsOperationErrorStatus.NOT_FOUND },
                { "OTHER",          Status.Code.ABORTED,    IdbmmsOperationErrorStatus.OTHER },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("fromThrowableDataProvider")
    void fromThrowableTest(String testCaseName, Status.Code code, IdbmmsOperationErrorStatus errorStatusExpected) {
        assertThat(IdbmmsOperationErrorStatus.fromThrowable(createStatusRuntimeException(code))).isEqualTo(errorStatusExpected);
    }

    @Test
    void fromThrowableTestNull() {
        assertThrows(NullPointerException.class, () -> IdbmmsOperationErrorStatus.fromThrowable(null));
    }

    private StatusRuntimeException createStatusRuntimeException(Status.Code code) {
        return new StatusRuntimeException(code.toStatus());
    }

}