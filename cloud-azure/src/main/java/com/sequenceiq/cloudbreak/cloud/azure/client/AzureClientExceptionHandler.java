package com.sequenceiq.cloudbreak.cloud.azure.client;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.aad.msal4j.MsalServiceException;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;

@Aspect
public class AzureClientExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureClientExceptionHandler.class);

    private AzureExceptionHandler azureExceptionHandler;

    public AzureClientExceptionHandler(AzureExceptionHandler azureExceptionHandler) {
        this.azureExceptionHandler = azureExceptionHandler;
    }

    @Around("execution(* *(..))")
    public Object handleUnhandledExceptions(ProceedingJoinPoint proceedingJoinPoint) {
        try {
            return proceedingJoinPoint.proceed();
        } catch (MsalServiceException e) {
            LOGGER.warn("Unhandled MsalServiceException has been thrown during azure operation", e);
            if (azureExceptionHandler.isUnauthorized(e)) {
                throw new ProviderAuthenticationFailedException(e.getMessage());
            }
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
