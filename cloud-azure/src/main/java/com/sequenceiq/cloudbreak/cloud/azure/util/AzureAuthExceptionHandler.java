package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.function.Supplier;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.aad.adal4j.AuthenticationException;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;

@Component
public class AzureAuthExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAuthExceptionHandler.class);

    @Inject
    private AzureExceptionExtractor azureExceptionExtractor;

    public <T> T handleAuthException(Supplier<T> function) {
        try {
            return function.get();
        } catch (RuntimeException e) {
            if (ExceptionUtils.indexOfThrowable(e, AuthenticationException.class) != -1) {
                LOGGER.warn("AuthenticationException has thrown during azure operation", e);
                throw new ProviderAuthenticationFailedException(azureExceptionExtractor.extractErrorMessage(e));
            } else {
                throw e;
            }
        }
    }

    public void handleAuthException(Runnable function) {
        try {
            function.run();
        } catch (RuntimeException e) {
            if (ExceptionUtils.indexOfThrowable(e, AuthenticationException.class) != -1) {
                LOGGER.warn("AuthenticationException has thrown during azure operation", e);
                throw new ProviderAuthenticationFailedException(azureExceptionExtractor.extractErrorMessage(e));
            } else {
                throw e;
            }
        }
    }
}
