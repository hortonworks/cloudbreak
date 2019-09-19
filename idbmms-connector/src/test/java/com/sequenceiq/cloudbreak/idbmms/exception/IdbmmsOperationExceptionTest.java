package com.sequenceiq.cloudbreak.idbmms.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

class IdbmmsOperationExceptionTest {

    private static final String MESSAGE = "message";

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] getErrorStatusDataProvider() {
        return new Object[][] {
                // testCaseName             withCause   code                    errorStatusExpected
                { "no cause",               false,      null,                   IdbmmsOperationErrorStatus.UNSPECIFIED },
                { "with cause null",        true,       null,                   IdbmmsOperationErrorStatus.UNSPECIFIED },
                { "with cause NOT_FOUND",   true,       Status.Code.NOT_FOUND,  IdbmmsOperationErrorStatus.NOT_FOUND },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("getErrorStatusDataProvider")
    void getErrorStatusTest(String testCaseName, boolean withCause, Status.Code code, IdbmmsOperationErrorStatus errorStatusExpected) {
        IdbmmsOperationException e;
        if (withCause) {
            Throwable cause = code == null ? null : createStatusRuntimeException(code);
            e = new IdbmmsOperationException(MESSAGE, cause);
        } else {
            e = new IdbmmsOperationException(MESSAGE);
        }
        assertThat(e.getErrorStatus()).isEqualTo(errorStatusExpected);
    }

    private StatusRuntimeException createStatusRuntimeException(Status.Code code) {
        return new StatusRuntimeException(code.toStatus());
    }

}