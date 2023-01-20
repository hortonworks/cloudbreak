package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.function.Supplier;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.core.management.exception.ManagementException;
import com.microsoft.aad.msal4j.MsalServiceException;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;

@Component
public class AzureExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureExceptionHandler.class);

    private static final int NOT_FOUND = 404;

    private static final int FORBIDDEN = 403;

    private static final int UNAUTHORIZED_CODE = 401;

    @Inject
    private AzureExceptionExtractor azureExceptionExtractor;

    public <T> T handleException(Supplier<T> function) {
        try {
            return function.get();
        } catch (MsalServiceException e) {
            LOGGER.warn("MsalServiceException has been thrown during azure operation", e);
            if (isUnauthorized(e)) {
                throw new ProviderAuthenticationFailedException(e.getMessage());
            } else {
                throw e;
            }
        } catch (ManagementException me) {
            LOGGER.warn("ManagementException has been thrown during azure operation", me);
            if (isNotFound(me)) {
                return null;
            }
            throw me;
        }
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
         * Azure SDK throws various codes such as "ResourceNotFound", "ResourceGroupNotFound", "DeploymnentNotFound"
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

    private boolean hasStatusCode(ManagementException exception, int statusCode) {
        return exception.getResponse() != null && statusCode == exception.getResponse().getStatusCode();
    }
}
