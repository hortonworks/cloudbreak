package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.core.management.exception.AdditionalInfo;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.microsoft.aad.msal4j.MsalServiceException;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureDeploymentCapacityError;
import com.sequenceiq.cloudbreak.cloud.exception.InsufficientCapacityException;

@Component
public class AzureExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureExceptionHandler.class);

    private static final String MGMT_ERROR_CODE_CONFLICT = "Conflict";

    private static final String CONCURRENT_WRITE_ERROR_CODE = "ConflictingConcurrentWriteNotAllowed";

    private static final String CONFLICTING_USER_INPUT_ERROR_CODE = "ConflictingUserInput";

    private static final String DEPLOYMENT_FAILED_ERROR_CODE = "DeploymentFailed";

    private static final String INTERNAL_SERVER_ERROR_CODE = "InternalServerError";

    private static final String UNEXPECTED_ERROR_MESSAGE_FRAGMENT = "unexpected error";

    private static final String ALREADY_ATTACHED_ERROR_MESSAGE = "cannot be attached as the disk is already owned by VM";

    private static final AzureExceptionHandlerParameters DEFAULT_EXCEPTION_HANDLER_PARAMETERS = AzureExceptionHandlerParameters.builder()
            .withHandleNotFound(true)
            .build();

    private static final int NOT_FOUND = 404;

    private static final int FORBIDDEN = 403;

    private static final int UNAUTHORIZED_CODE = 401;

    private static final int CONFLICT = 409;

    @Inject
    private AzureCapacityErrorMessageProvider azureCapacityErrorMessageProvider;

    public void handleException(Runnable function) {
        Supplier<Void> supplier = () -> {
            function.run();
            return null;
        };
        handleException(supplier, null, AzureExceptionHandlerParameters.builder().build());
    }

    public <T> Optional<T> handleException(Supplier<T> function) {
        return handleException(function, DEFAULT_EXCEPTION_HANDLER_PARAMETERS);
    }

    public <T> T handleException(Supplier<T> function, T defaultValue) {
        return handleException(function, defaultValue, DEFAULT_EXCEPTION_HANDLER_PARAMETERS);
    }

    public <T> T handleException(Supplier<T> function, T defaultValue, AzureExceptionHandlerParameters azureExceptionHandlerParameters) {
        return handleException(function, azureExceptionHandlerParameters)
                .orElse(defaultValue);
    }

    private <T> Optional<T> handleException(Supplier<T> function, AzureExceptionHandlerParameters azureExceptionHandlerParameters) {
        try {
            T result = function.get();
            return Optional.ofNullable(result);
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
                return Optional.empty();
            }
            if (azureExceptionHandlerParameters.isHandleNotFound() && isNotFound(me)) {
                LOGGER.debug("Handle not found exception is turned on");
                return Optional.empty();
            }
            if (isCapacityError(me)) {
                LOGGER.debug("Re-throwing as InsufficientCapacityException", me);
                throw new InsufficientCapacityException(AzureUtils.getErrorMessage(me, "Azure operation"), me);
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

    public boolean isCapacityError(ManagementException e) {
        return flattenErrors(e.getValue()).stream().anyMatch(this::isCapacityError);
    }

    private boolean isCapacityError(ManagementError error) {
        if (AzureDeploymentCapacityError.isCapacityErrorCode(error.getCode())) {
            return true;
        }
        if (error.getAdditionalInfo() != null) {
            for (AdditionalInfo info : error.getAdditionalInfo()) {
                if (AzureDeploymentCapacityError.isCapacityErrorCode(info.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isTransientProvisioningFailure(ManagementException e) {
        if (e == null || e.getValue() == null || !DEPLOYMENT_FAILED_ERROR_CODE.equalsIgnoreCase(e.getValue().getCode())) {
            return false;
        }
        boolean internalServerError = false;
        for (ManagementError error : flattenErrors(e.getValue())) {
            if (isCapacityError(error)) {
                return false;
            }
            internalServerError |= isInternalServerError(error);
        }
        return internalServerError;
    }

    private boolean isInternalServerError(ManagementError error) {
        return INTERNAL_SERVER_ERROR_CODE.equalsIgnoreCase(error.getCode())
                || StringUtils.containsIgnoreCase(error.getMessage(), UNEXPECTED_ERROR_MESSAGE_FRAGMENT);
    }

    private List<ManagementError> flattenErrors(ManagementError root) {
        if (root == null) {
            return List.of();
        }
        List<ManagementError> errors = new ArrayList<>();
        Deque<ManagementError> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            ManagementError current = stack.pop();
            errors.add(current);
            List<? extends ManagementError> details = current.getDetails();
            if (details != null) {
                for (ManagementError detail : details) {
                    stack.push(detail);
                }
            }
        }
        return errors;
    }

    public AzureCapacityErrorMessageProvider capacityErrorMessageProvider() {
        return azureCapacityErrorMessageProvider;
    }
}