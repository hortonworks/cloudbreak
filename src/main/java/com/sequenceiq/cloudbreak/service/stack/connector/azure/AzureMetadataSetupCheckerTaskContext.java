package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.Stack;

public class AzureMetadataSetupCheckerTaskContext {

    private AzureClient azureClient;
    private Stack stack;
    private Map<String, Object> props = new HashMap<>();

    public AzureMetadataSetupCheckerTaskContext(AzureClient azureClient, Stack stack,  Map<String, Object> props) {
        this.azureClient = azureClient;
        this.stack = stack;
        this.props = props;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public Stack getStack() {
        return stack;
    }

    public Map<String, Object> getProps() {
        return props;
    }
}
