package com.sequenceiq.cloudbreak.service.stack.resource.azure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

import groovyx.net.http.HttpResponseDecorator;

public class AzureResourcePollerObject extends StackContext {

    private AzureClient azureClient;
    private ResourceType resourceType;
    private String resourceName;
    private List<HttpResponseDecorator> responses;

    public AzureResourcePollerObject(AzureClient azureClient, ResourceType resourceType, String resourceName,
            Stack stack, HttpResponseDecorator... responses) {
        this(azureClient, resourceType, resourceName, stack, Arrays.asList(responses));
    }

    public AzureResourcePollerObject(AzureClient azureClient, ResourceType resourceType, String resourceName,
            Stack stack, List<HttpResponseDecorator> responses) {
        super(stack);
        this.resourceType = resourceType;
        this.resourceName = resourceName;
        this.responses = new ArrayList<>(responses);
        this.azureClient = azureClient;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public List<HttpResponseDecorator> getResponses() {
        return responses;
    }

    public ResourceType getType() {
        return resourceType;
    }

    public String getName() {
        return resourceName;
    }
}