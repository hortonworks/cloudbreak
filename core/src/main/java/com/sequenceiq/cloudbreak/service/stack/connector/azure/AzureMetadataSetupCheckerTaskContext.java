package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class AzureMetadataSetupCheckerTaskContext extends StackContext {

    private AzureClient azureClient;
    private Map<String, Object> props = new HashMap<>();

    public AzureMetadataSetupCheckerTaskContext(AzureClient azureClient, Stack stack,  Map<String, Object> props) {
        super(stack);
        this.azureClient = azureClient;
        this.props = props;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public Map<String, Object> getProps() {
        return props;
    }
}
