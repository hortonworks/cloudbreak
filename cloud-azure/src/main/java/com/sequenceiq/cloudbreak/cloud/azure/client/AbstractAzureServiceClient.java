package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.util.function.Supplier;

import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;

public abstract class AbstractAzureServiceClient {

    private final AzureListResultFactory azureListResultFactory;

    private final AzureExceptionHandler azureExceptionHandler;

    protected AbstractAzureServiceClient(AzureExceptionHandler azureExceptionHandler, AzureListResultFactory azureListResultFactory) {
        this.azureExceptionHandler = azureExceptionHandler;
        this.azureListResultFactory = azureListResultFactory;
    }

    protected <T> T handleException(Supplier<T> function) {
        return azureExceptionHandler.handleException(function);
    }

    protected <T> T handleException(Supplier<T> function, T defaultValue) {
        return azureExceptionHandler.handleException(function, defaultValue);
    }

    protected void handleException(Runnable function) {
        azureExceptionHandler.handleException(function);
    }

    protected AzureListResultFactory getAzureListResultFactory() {
        return azureListResultFactory;
    }
}
