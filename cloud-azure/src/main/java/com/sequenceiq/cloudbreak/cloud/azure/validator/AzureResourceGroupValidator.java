package com.sequenceiq.cloudbreak.cloud.azure.validator;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Component
public class AzureResourceGroupValidator implements Validator {

    @Inject
    private AzureUtils azureUtils;

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        AzureClient client = ac.getParameter(AzureClient.class);
        String resourceGroupName = azureUtils.getResourceGroupName(ac.getCloudContext(), cloudStack);
        boolean exists = client.resourceGroupExists(resourceGroupName);
        if (exists) {
            throw new CloudConnectorException(String.format("Resource group is already exists with the given name: %s", resourceGroupName));
        }
    }
}
