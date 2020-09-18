package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.function.Supplier;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;

import com.microsoft.aad.adal4j.AuthenticationException;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;

@Component
public class AzureAuthExceptionHandler {

    public <T> T handleAuthException(Supplier<T> function) {
        try {
            return function.get();
        } catch (RuntimeException e) {
            if (ExceptionUtils.indexOfThrowable(e, AuthenticationException.class) != -1) {
                throw new ProviderAuthenticationFailedException(e);
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
                throw new ProviderAuthenticationFailedException(e);
            } else {
                throw e;
            }
        }
    }
}
