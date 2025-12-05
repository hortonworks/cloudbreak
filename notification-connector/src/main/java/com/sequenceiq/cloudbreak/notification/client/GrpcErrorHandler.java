package com.sequenceiq.cloudbreak.notification.client;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * Centralized gRPC error handling service.
 */
public class GrpcErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcErrorHandler.class);

    /**
     * Execute a gRPC operation with comprehensive error handling.
     *
     * @param operation the operation to execute
     * @param operationDescription description of the operation for logging
     * @param contextInfo additional context information for error messages
     * @param <T> the return type of the operation
     * @return the result of the operation
     * @throws NotificationServiceException if an error occurs
     */
    public <T> T executeWithErrorHandling(Supplier<T> operation, String operationDescription, String contextInfo) {
        try {
            return operation.get();
        } catch (StatusRuntimeException ex) {
            throw handleStatusRuntimeException(ex, operationDescription, contextInfo);
        } catch (Exception ex) {
            if (ex instanceof NotificationServiceException) {
                throw ex;
            }
            return handleUnexpectedException(ex, operationDescription, contextInfo);
        }
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    private RuntimeException handleStatusRuntimeException(StatusRuntimeException ex, String operationDescription, String contextInfo) {
        Status.Code statusCode = ex.getStatus().getCode();
        String statusDescription = ex.getStatus().getDescription();
        String errorDetails = String.format("Failed to %s for %s. Status: %s, Description: %s",
                operationDescription, contextInfo, statusCode, statusDescription);

        return switch (statusCode) {
            case UNAVAILABLE -> {
                LOGGER.warn("{} - Service unavailable, may retry. Trailers: {}", errorDetails, ex.getTrailers(), ex);
                yield new NotificationServiceUnavailableException(
                        String.format("Notification service unavailable while %s: %s", operationDescription, contextInfo), ex);
            }
            case DEADLINE_EXCEEDED -> {
                LOGGER.warn("{} - Request timed out, may retry. Trailers: {}", errorDetails, ex.getTrailers(), ex);
                yield new NotificationServiceTimeoutException(
                        String.format("Notification service timeout while %s: %s", operationDescription, contextInfo), ex);
            }
            case RESOURCE_EXHAUSTED -> {
                LOGGER.warn("{} - Rate limit or quota exceeded, should retry with backoff. Trailers: {}",
                        errorDetails, ex.getTrailers(), ex);
                yield new NotificationServiceRateLimitException(
                        String.format("Notification service rate limit exceeded while %s: %s", operationDescription, contextInfo), ex);
            }
            case CANCELLED -> {
                LOGGER.warn("{} - Request cancelled. Trailers: {}", errorDetails, ex.getTrailers(), ex);
                yield new NotificationServiceException(
                        String.format("Request cancelled while %s: %s", operationDescription, contextInfo), ex);
            }
            case NOT_FOUND -> {
                LOGGER.error("{} - Resource not found. Trailers: {}", errorDetails, ex.getTrailers(), ex);
                yield new NotificationServiceException(
                        String.format("Resource not found while %s: %s", operationDescription, contextInfo), ex);
            }
            case ALREADY_EXISTS -> {
                LOGGER.error("{} - Resource already exists. Trailers: {}", errorDetails, ex.getTrailers(), ex);
                yield new NotificationServiceException(
                        String.format("Resource already exists while %s: %s", operationDescription, contextInfo), ex);
            }
            case PERMISSION_DENIED, UNAUTHENTICATED -> {
                LOGGER.error("{} - Authentication/authorization failure. Trailers: {}", errorDetails, ex.getTrailers(), ex);
                yield new NotificationServiceException(
                        String.format("Authentication/authorization failed while %s: %s", operationDescription, contextInfo), ex);
            }
            case INVALID_ARGUMENT, FAILED_PRECONDITION -> {
                LOGGER.error("{} - Invalid request. Trailers: {}", errorDetails, ex.getTrailers(), ex);
                yield new NotificationServiceException(
                        String.format("Invalid request while %s: %s. Description: %s",
                                operationDescription, contextInfo, statusDescription), ex);
            }
            case INTERNAL, UNKNOWN, DATA_LOSS -> {
                LOGGER.error("{} - Internal server error. Trailers: {}", errorDetails, ex.getTrailers(), ex);
                yield new NotificationServiceException(
                        String.format("Internal server error while %s: %s", operationDescription, contextInfo), ex);
            }
            case UNIMPLEMENTED -> {
                LOGGER.error("{} - Operation not implemented. Trailers: {}", errorDetails, ex.getTrailers(), ex);
                yield new NotificationServiceException(
                        String.format("Operation not implemented while %s: %s", operationDescription, contextInfo), ex);
            }
            default -> {
                LOGGER.error("{} - Unexpected gRPC status. Trailers: {}", errorDetails, ex.getTrailers(), ex);
                yield new NotificationServiceException(errorDetails, ex);
            }
        };
    }

    private <T> T handleUnexpectedException(Exception ex, String operationDescription, String contextInfo) {
        String errorMessage = String.format("Unexpected error while %s for %s", operationDescription, contextInfo);
        LOGGER.error(errorMessage, ex);
        throw new NotificationServiceException(errorMessage, ex);
    }

    public static class NotificationServiceException extends RuntimeException {
        public NotificationServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class NotificationServiceUnavailableException extends NotificationServiceException {
        public NotificationServiceUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class NotificationServiceTimeoutException extends NotificationServiceException {
        public NotificationServiceTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class NotificationServiceRateLimitException extends NotificationServiceException {
        public NotificationServiceRateLimitException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

