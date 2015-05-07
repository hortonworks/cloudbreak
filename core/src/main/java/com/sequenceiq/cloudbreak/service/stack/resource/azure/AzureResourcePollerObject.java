package com.sequenceiq.cloudbreak.service.stack.resource.azure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

import groovyx.net.http.HttpResponseDecorator;

public class AzureResourcePollerObject extends StackContext {

    private AzureClient azureClient;
    private List<HttpResponseDecorator> responses;

    public AzureResourcePollerObject(AzureClient azureClient, Stack stack, HttpResponseDecorator... responses) {
        this(azureClient, stack, Arrays.asList(responses));
    }

    public AzureResourcePollerObject(AzureClient azureClient, Stack stack, List<HttpResponseDecorator> responses) {
        super(stack);
        this.responses = new ArrayList<>(responses);
        this.azureClient = azureClient;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public void setAzureClient(AzureClient azureClient) {
        this.azureClient = azureClient;
    }

    public List<HttpResponseDecorator> getResponses() {
        return responses;
    }

    public void setResponses(List<HttpResponseDecorator> responses) {
        this.responses = responses;
    }

}