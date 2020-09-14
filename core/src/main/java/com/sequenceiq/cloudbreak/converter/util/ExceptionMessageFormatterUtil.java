package com.sequenceiq.cloudbreak.converter.util;

import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

public class ExceptionMessageFormatterUtil {

    private ExceptionMessageFormatterUtil() {
    }

    public static void formatAccessDeniedMessage(Runnable r, String resourceName, Long id) {
        try {
            r.run();
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(
                    String.format("Access to %s '%s' is denied or %s doesn't exist.", resourceName, id, resourceName), e);
        }
    }

    public static void formatAccessDeniedMessage(Runnable r, String resourceType, String resourceName) {
        try {
            r.run();
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(
                    String.format("Access to %s '%s' is denied or %s doesn't exist.", resourceType, resourceName, resourceType), e);
        }
    }

    public static String getErrorMessageFromException(Exception exception) {
        boolean transactionRuntimeException = exception instanceof TransactionService.TransactionRuntimeExecutionException;
        if (transactionRuntimeException && exception.getCause() != null && exception.getCause().getCause() != null) {
            return exception.getCause().getCause().getMessage();
        } else {
            return exception instanceof CloudbreakException && exception.getCause() != null
                    ? exception.getCause().getMessage() : exception.getMessage();
        }
    }
}
