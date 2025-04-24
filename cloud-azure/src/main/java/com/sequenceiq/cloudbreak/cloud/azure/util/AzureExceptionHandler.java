package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.microsoft.aad.msal4j.MsalServiceException;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;

@Component
public class AzureExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureExceptionHandler.class);

    private static final String MGMT_ERROR_CODE_CONFLICT = "Conflict";

    private static final String CONCURRENT_WRITE_ERROR_CODE = "ConflictingConcurrentWriteNotAllowed";

    private static final String CONFLICTING_USER_INPUT_ERROR_CODE = "ConflictingUserInput";

    private static final String ALREADY_ATTACHED_ERROR_MESSAGE = "cannot be attached as the disk is already owned by VM";

    private static final AzureExceptionHandlerParameters DEFAULT_EXCEPTION_HANDLER_PARAMETERS = AzureExceptionHandlerParameters.builder()
            .withHandleNotFound(true)
            .build();

    private static final int NOT_FOUND = 404;

    private static final int FORBIDDEN = 403;

    private static final int UNAUTHORIZED_CODE = 401;

    private static final int CONFLICT = 409;

    public <T> T handleException(Supplier<T> function) {
        return handleException(function, DEFAULT_EXCEPTION_HANDLER_PARAMETERS);
    }

    public <T> T handleException(Supplier<T> function, T defaultValue) {
        return handleException(function, defaultValue, DEFAULT_EXCEPTION_HANDLER_PARAMETERS);
    }

    public <T> T handleException(Supplier<T> function, T defaultValue, AzureExceptionHandlerParameters azureExceptionHandlerParameters) {
        return Optional.ofNullable(handleException(function, azureExceptionHandlerParameters))
                .orElse(defaultValue);
    }

    private <T> T handleException(Supplier<T> function, AzureExceptionHandlerParameters azureExceptionHandlerParameters) {
        try {
            return function.get();
        } catch (MsalServiceException e) {
            LOGGER.warn("MsalServiceException has been thrown during azure operation", e);
            if (isUnauthorized(e) || isForbidden(e)) {
                throw new ProviderAuthenticationFailedException(e.getMessage());
            } else {
                throw e;
            }
        } catch (ManagementException me) {
            LOGGER.warn("ManagementException has been thrown during azure operation", me);
            logErrorMessage(me);

            if (azureExceptionHandlerParameters.isHandleAllExceptions()) {
                LOGGER.debug("Handle all exceptions is turned on");
                return null;
            }
            if (azureExceptionHandlerParameters.isHandleNotFound() && isNotFound(me)) {
                LOGGER.debug("Handle not found exception is turned on");
                return null;
            }
            throw me;
        }
    }

    public void logErrorMessage(ManagementException me) {
        StringJoiner errorMessageBuilder = new StringJoiner(". ");
        errorMessageBuilder.add("Azure management error happened");
        errorMessageBuilder.add(me.getMessage());
        ManagementError managementError = me.getValue();
        if (managementError != null) {
            LOGGER.warn("Azure management error: {}", managementError);
            errorMessageBuilder.add(managementError.getMessage());
            if (managementError.getDetails() != null) {
                LOGGER.warn("Azure management error detail: {}", managementError.getDetails());
                for (ManagementError detail : managementError.getDetails()) {
                    errorMessageBuilder.add(detail.getMessage());
                }
            }
        }
        LOGGER.warn(errorMessageBuilder.toString());
    }

    public void handleException(Runnable function) {
        try {
            function.run();
        } catch (MsalServiceException e) {
            if (UNAUTHORIZED_CODE == e.statusCode()) {
                LOGGER.warn("AuthenticationException has thrown during azure operation", e);
                throw new ProviderAuthenticationFailedException(e.getMessage());
            } else {
                throw e;
            }
        }
    }

    public boolean isNotFound(ManagementException exception) {
        /*
         * Azure SDK throws various codes such as "ResourceNotFound", "ResourceGroupNotFound", "DeploymentNotFound"
         * when a resource doesn't exist. Instead of checking the code in a fragile way the below part checks only the
         * status code which is always 404 if the resource doesn't exist.
         */
        return hasStatusCode(exception, NOT_FOUND);
    }

    public boolean isForbidden(ManagementException exception) {
        return hasStatusCode(exception, FORBIDDEN);
    }

    public boolean isUnauthorized(MsalServiceException msalServiceException) {
        return UNAUTHORIZED_CODE == msalServiceException.statusCode();
    }

    public boolean isForbidden(MsalServiceException msalServiceException) {
        return FORBIDDEN == msalServiceException.statusCode();
    }

    public boolean isConcurrentWrite(ManagementException ex) {
        return httpAndAzureStatusCodeMatches(CONFLICT, CONCURRENT_WRITE_ERROR_CODE, Optional.empty(), ex);
    }

    public boolean isDiskAlreadyAttached(ManagementException ex) {
        return httpAndAzureStatusCodeMatches(CONFLICT, CONFLICTING_USER_INPUT_ERROR_CODE, Optional.of(ALREADY_ATTACHED_ERROR_MESSAGE), ex);
    }

    private boolean hasStatusCode(ManagementException exception, int statusCode) {
        return exception.getResponse() != null && statusCode == exception.getResponse().getStatusCode();
    }

    public boolean isExceptionCodeConflict(ManagementException e) {
        return e != null && e.getValue() != null
                && (MGMT_ERROR_CODE_CONFLICT.equalsIgnoreCase(e.getValue().getCode()) || isDetailsContainAnyConflict(e));
    }

    private boolean isDetailsContainAnyConflict(ManagementException e) {
        return e.getValue().getDetails() != null
                && e.getValue().getDetails().stream().anyMatch(detail -> MGMT_ERROR_CODE_CONFLICT.equalsIgnoreCase(detail.getCode()));
    }

    private boolean httpAndAzureStatusCodeMatches(Integer expectedHttpStatusCode, String expectedAzureErrorCode,
            Optional<String> expectedAzureErrorMessage, ManagementException exception) {
        ManagementError error = exception.getValue();
        return exception.getResponse() != null && exception.getResponse().getStatusCode() == expectedHttpStatusCode && error != null &&
                error.getCode().contains(expectedAzureErrorCode) &&
                expectedAzureErrorMessage.stream().allMatch(errorMessage -> error.getMessage() != null && error.getMessage().contains(errorMessage));
    }
}