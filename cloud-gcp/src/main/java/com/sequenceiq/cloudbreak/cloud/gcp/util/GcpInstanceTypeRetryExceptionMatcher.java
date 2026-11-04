package com.sequenceiq.cloudbreak.cloud.gcp.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.model.ErrorInfo;
import com.google.api.services.compute.model.Operation;

public class GcpInstanceTypeRetryExceptionMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpInstanceTypeRetryExceptionMatcher.class);

    private static final String UNKNOWN = "Unknown";

    private GcpInstanceTypeRetryExceptionMatcher() {
    }

    public static boolean isInstanceTypeNotSupported(GoogleJsonResponseException e) {
        Optional<GcpInstanceTypeUnavailableError> exceptionMessage = findInstanceTypeUnavailableError(e);
        exceptionMessage.ifPresent(unavailableError -> LOGGER.info("Instance type related error ({}) during instance creation. status: {}, message: {}",
                unavailableError.getCode(), e.getStatusCode(), e.getDetails().getMessage()));
        return exceptionMessage.isPresent();
    }

    public static boolean isInstanceTypeNotSupported(Operation operation) {
        Optional<GcpInstanceTypeUnavailableError> exceptionMessage = findInstanceTypeUnavailableError(operation);
        exceptionMessage.ifPresent(unavailableError -> LOGGER.info("Instance type related error ({}) in operation {}.",
                unavailableError.getCode(), operation.getName()));
        return exceptionMessage.isPresent();
    }

    public static String getGcpErrorCodeForNotification(GoogleJsonResponseException e) {
        if (e == null || e.getDetails() == null) {
            return UNKNOWN;
        }
        return findInstanceTypeUnavailableError(e)
                .map(GcpInstanceTypeUnavailableError::getCode)
                .orElse("HTTP_" + e.getStatusCode());
    }

    public static String getGcpErrorCodeForNotification(Operation operation) {
        return findInstanceTypeUnavailableError(operation)
                .map(GcpInstanceTypeUnavailableError::getCode)
                .or(() -> operationErrors(operation).map(Operation.Error.Errors::getCode).filter(Objects::nonNull).findFirst())
                .orElse(UNKNOWN);
    }

    private static Optional<GcpInstanceTypeUnavailableError> findInstanceTypeUnavailableError(GoogleJsonResponseException e) {
        if (e == null || e.getDetails() == null) {
            return Optional.empty();
        }
        GoogleJsonError exceptionDetails = e.getDetails();
        List<String> exceptionMessages = new ArrayList<>();
        exceptionMessages.add(exceptionDetails.getMessage());
        Optional.ofNullable(exceptionDetails.getErrors()).orElse(List.of()).stream()
                .filter(Objects::nonNull)
                .forEach(errorInfo -> {
                    exceptionMessages.add(errorInfo.getReason());
                    exceptionMessages.add(errorInfo.getMessage());
                });
        Optional.ofNullable(exceptionDetails.getDetails()).orElse(List.of()).stream()
                .filter(Objects::nonNull)
                .forEach(detail -> {
                    exceptionMessages.add(detail.getReason());
                    exceptionMessages.add(detail.getDetail());
                });
        return findFirstMatch(exceptionMessages);
    }

    private static Optional<GcpInstanceTypeUnavailableError> findInstanceTypeUnavailableError(Operation operation) {
        List<String> errorMessages = operationErrors(operation)
                .flatMap(error -> Stream.concat(Stream.of(error.getCode(), error.getMessage()), errorInfoReasons(error)))
                .toList();
        return findFirstMatch(errorMessages);
    }

    private static Stream<Operation.Error.Errors> operationErrors(Operation operation) {
        if (operation == null || operation.getError() == null || operation.getError().getErrors() == null) {
            return Stream.empty();
        }
        return operation.getError().getErrors().stream().filter(Objects::nonNull);
    }

    private static Stream<String> errorInfoReasons(Operation.Error.Errors error) {
        return Optional.ofNullable(error.getErrorDetails()).orElse(List.of()).stream()
                .filter(Objects::nonNull)
                .map(Operation.Error.Errors.ErrorDetails::getErrorInfo)
                .filter(Objects::nonNull)
                .map(ErrorInfo::getReason);
    }

    private static Optional<GcpInstanceTypeUnavailableError> findFirstMatch(List<String> exceptionMessageTexts) {
        List<String> nonNullExceptionMessages = exceptionMessageTexts.stream().filter(Objects::nonNull).toList();
        return Arrays.stream(GcpInstanceTypeUnavailableError.values())
                .filter(error -> nonNullExceptionMessages.stream().anyMatch(error::matches))
                .findFirst();
    }
}
