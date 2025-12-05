package com.sequenceiq.cloudbreak.notification.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.notification.client.GrpcErrorHandler.NotificationServiceException;
import com.sequenceiq.cloudbreak.notification.client.GrpcErrorHandler.NotificationServiceRateLimitException;
import com.sequenceiq.cloudbreak.notification.client.GrpcErrorHandler.NotificationServiceTimeoutException;
import com.sequenceiq.cloudbreak.notification.client.GrpcErrorHandler.NotificationServiceUnavailableException;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

class GrpcErrorHandlerTest {

    private static final String OPERATION_DESCRIPTION = "test operation";

    private static final String CONTEXT_INFO = "resource: test-resource";

    private GrpcErrorHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new GrpcErrorHandler();
    }

    @Test
    void executeWithErrorHandlingReturnsSuccessfulResult() {
        String expectedResult = "success";

        String result = underTest.executeWithErrorHandling(() -> expectedResult, OPERATION_DESCRIPTION, CONTEXT_INFO);

        assertEquals(expectedResult, result);
    }

    @Test
    void executeWithErrorHandlingReturnsNullResult() {
        String result = underTest.executeWithErrorHandling(() -> null, OPERATION_DESCRIPTION, CONTEXT_INFO);

        assertNull(result);
    }

    @ParameterizedTest
    @MethodSource("provideRetriableStatusCodes")
    void retriableStatusCodesThrowSpecificExceptions(Status.Code statusCode, Class<? extends NotificationServiceException> expectedExceptionType,
            String expectedMessageFragment, String description) {
        StatusRuntimeException exception = new StatusRuntimeException(statusCode.toStatus().withDescription(description));

        NotificationServiceException result = assertThrows(expectedExceptionType,
                () -> underTest.executeWithErrorHandling(() -> {
                    throw exception;
                }, OPERATION_DESCRIPTION, CONTEXT_INFO));

        assertTrue(result.getMessage().contains(expectedMessageFragment));
        assertTrue(result.getMessage().contains(OPERATION_DESCRIPTION));
        assertTrue(result.getMessage().contains(CONTEXT_INFO));
        assertInstanceOf(StatusRuntimeException.class, result.getCause());
        assertEquals(statusCode, ((StatusRuntimeException) result.getCause()).getStatus().getCode());
    }

    private static Stream<Arguments> provideRetriableStatusCodes() {
        return Stream.of(
                Arguments.of(Status.Code.UNAVAILABLE, NotificationServiceUnavailableException.class, "unavailable", "Service unavailable"),
                Arguments.of(Status.Code.DEADLINE_EXCEEDED, NotificationServiceTimeoutException.class, "timeout", "Request timeout"),
                Arguments.of(Status.Code.RESOURCE_EXHAUSTED, NotificationServiceRateLimitException.class, "rate limit", "Rate limit exceeded")
        );
    }

    @ParameterizedTest
    @EnumSource(value = Status.Code.class, names = {"NOT_FOUND", "ALREADY_EXISTS", "CANCELLED",
            "PERMISSION_DENIED", "UNAUTHENTICATED", "INVALID_ARGUMENT", "FAILED_PRECONDITION",
            "INTERNAL", "UNKNOWN", "DATA_LOSS", "UNIMPLEMENTED", "ABORTED", "OUT_OF_RANGE"})
    void nonRetriableStatusesThrowBaseException(Status.Code statusCode) {
        StatusRuntimeException exception = new StatusRuntimeException(statusCode.toStatus().withDescription("Error occurred"));

        NotificationServiceException result = assertThrows(NotificationServiceException.class,
                () -> underTest.executeWithErrorHandling(() -> {
                    throw exception;
                }, OPERATION_DESCRIPTION, CONTEXT_INFO));

        assertNotNull(result.getMessage());
        assertInstanceOf(StatusRuntimeException.class, result.getCause());
        assertEquals(statusCode, ((StatusRuntimeException) result.getCause()).getStatus().getCode());
    }

    @ParameterizedTest
    @EnumSource(NonGrpcException.class)
    void nonGrpcExceptionsThrowNotificationServiceException(NonGrpcException exceptionType) {
        RuntimeException originalException = exceptionType.createException();

        NotificationServiceException exception = assertThrows(NotificationServiceException.class,
                () -> underTest.executeWithErrorHandling(() -> {
                    throw originalException;
                }, OPERATION_DESCRIPTION, CONTEXT_INFO));

        assertTrue(exception.getMessage().contains("Unexpected error"));
        assertTrue(exception.getMessage().contains(OPERATION_DESCRIPTION));
        assertTrue(exception.getMessage().contains(CONTEXT_INFO));
        assertEquals(originalException, exception.getCause());
    }

    @ParameterizedTest
    @EnumSource(NotificationExceptionType.class)
    void notificationServiceExceptionsAreRethrown(NotificationExceptionType exceptionType) {
        RuntimeException originalException = exceptionType.createException();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> underTest.executeWithErrorHandling(() -> {
                    throw originalException;
                }, OPERATION_DESCRIPTION, CONTEXT_INFO));

        assertEquals(originalException, exception);
    }

    @Test
    void exceptionHierarchyIsCorrect() {
        NotificationServiceException baseException = new NotificationServiceException("Base", null);
        NotificationServiceUnavailableException unavailableException = new NotificationServiceUnavailableException("Unavailable", null);
        NotificationServiceTimeoutException timeoutException = new NotificationServiceTimeoutException("Timeout", null);
        NotificationServiceRateLimitException rateLimitException = new NotificationServiceRateLimitException("Rate limit", null);

        assertInstanceOf(RuntimeException.class, baseException);
        assertInstanceOf(NotificationServiceException.class, unavailableException);
        assertInstanceOf(NotificationServiceException.class, timeoutException);
        assertInstanceOf(NotificationServiceException.class, rateLimitException);
    }

    @ParameterizedTest
    @EnumSource(value = Status.Code.class, names = {"UNAVAILABLE", "DEADLINE_EXCEEDED", "RESOURCE_EXHAUSTED"})
    void retriableErrorsAreIdentified(Status.Code statusCode) {
        StatusRuntimeException exception = new StatusRuntimeException(statusCode.toStatus());

        RuntimeException result = assertThrows(RuntimeException.class,
                () -> underTest.executeWithErrorHandling(() -> {
                    throw exception;
                }, OPERATION_DESCRIPTION, CONTEXT_INFO));

        assertTrue(result instanceof NotificationServiceUnavailableException
                || result instanceof NotificationServiceTimeoutException
                || result instanceof NotificationServiceRateLimitException);
    }

    @ParameterizedTest
    @EnumSource(value = Status.Code.class, names = {"NOT_FOUND", "ALREADY_EXISTS", "PERMISSION_DENIED",
            "INVALID_ARGUMENT", "INTERNAL", "UNIMPLEMENTED"})
    void nonRetriableErrorsThrowBaseException(Status.Code statusCode) {
        StatusRuntimeException exception = new StatusRuntimeException(statusCode.toStatus());

        NotificationServiceException result = assertThrows(NotificationServiceException.class,
                () -> underTest.executeWithErrorHandling(() -> {
                    throw exception;
                }, OPERATION_DESCRIPTION, CONTEXT_INFO));

        assertNotNull(result);
        assertInstanceOf(StatusRuntimeException.class, result.getCause());
    }

    @Test
    void operationDescriptionIsIncludedInErrorMessage() {
        String customOperation = "custom publish event";
        String customContext = "resource: custom-resource-123";
        StatusRuntimeException exception = new StatusRuntimeException(Status.UNAVAILABLE);

        NotificationServiceException result = assertThrows(NotificationServiceException.class,
                () -> underTest.executeWithErrorHandling(() -> {
                    throw exception;
                }, customOperation, customContext));

        assertTrue(result.getMessage().contains(customOperation));
        assertTrue(result.getMessage().contains(customContext));

    }

    enum NonGrpcException {
        RUNTIME_EXCEPTION {
            @Override
            RuntimeException createException() {
                return new RuntimeException("Generic error");
            }
        },
        NULL_POINTER_EXCEPTION {
            @Override
            RuntimeException createException() {
                return new NullPointerException("Null value");
            }
        },
        ILLEGAL_ARGUMENT_EXCEPTION {
            @Override
            RuntimeException createException() {
                return new IllegalArgumentException("Invalid argument");
            }
        },
        ILLEGAL_STATE_EXCEPTION {
            @Override
            RuntimeException createException() {
                return new IllegalStateException("Invalid state");
            }
        };

        abstract RuntimeException createException();
    }

    enum NotificationExceptionType {
        BASE_EXCEPTION {
            @Override
            RuntimeException createException() {
                return new NotificationServiceException("Original error", null);
            }
        },
        UNAVAILABLE_EXCEPTION {
            @Override
            RuntimeException createException() {
                return new NotificationServiceUnavailableException("Service down", null);
            }
        },
        TIMEOUT_EXCEPTION {
            @Override
            RuntimeException createException() {
                return new NotificationServiceTimeoutException("Timeout", null);
            }
        },
        RATE_LIMIT_EXCEPTION {
            @Override
            RuntimeException createException() {
                return new NotificationServiceRateLimitException("Rate limit", null);
            }
        };

        abstract RuntimeException createException();
    }
}