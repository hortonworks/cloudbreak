package com.sequenceiq.cloudbreak.converter.util;

import org.springframework.security.access.AccessDeniedException;

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
}
