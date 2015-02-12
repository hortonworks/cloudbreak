package com.sequenceiq.cloudbreak.service.stack.resource.azure;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackDependentPollerObject;

import groovyx.net.http.HttpResponseDecorator;

public class AzureResourcePollerObject extends StackDependentPollerObject {

    private AzureClient azureClient;
    private HttpResponseDecorator httpResponseDecorator;

    public AzureResourcePollerObject(AzureClient azureClient, HttpResponseDecorator httpResponseDecorator, Stack stack) {
        super(stack);
        this.azureClient = azureClient;
        this.httpResponseDecorator = httpResponseDecorator;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public HttpResponseDecorator getHttpResponseDecorator() {
        return httpResponseDecorator;
    }
}