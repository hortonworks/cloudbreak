package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.util.function.Supplier;

import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;

public abstract class AbstractAzureServiceClient {
    private final AzureExceptionHandler azureExceptionHandler;

    protected AbstractAzureServiceClient(AzureExceptionHandler azureExceptionHandler) {
        this.azureExceptionHandler = azureExceptionHandler;
    }

    protected <T> T handleException(Supplier<T> function) {
        return azureExceptionHandler.handleException(function);
    }

    protected void handleException(Runnable function) {
        azureExceptionHandler.handleException(function);
    }
}
