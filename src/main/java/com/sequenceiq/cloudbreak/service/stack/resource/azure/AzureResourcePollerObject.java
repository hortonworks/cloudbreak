package com.sequenceiq.cloudbreak.service.stack.resource.azure;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.Stack;

import groovyx.net.http.HttpResponseDecorator;

public class AzureResourcePollerObject {

    private AzureClient azureClient;
    private HttpResponseDecorator httpResponseDecorator;
    private Stack stack;

    public AzureResourcePollerObject(AzureClient azureClient, HttpResponseDecorator httpResponseDecorator, Stack stack) {
        this.azureClient = azureClient;
        this.httpResponseDecorator = httpResponseDecorator;
        this.stack = stack;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public Stack getStack() {
        return stack;
    }

    public HttpResponseDecorator getHttpResponseDecorator() {
        return httpResponseDecorator;
    }
}