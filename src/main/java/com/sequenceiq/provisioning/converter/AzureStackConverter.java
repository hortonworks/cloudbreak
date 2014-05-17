package com.sequenceiq.provisioning.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.ProvisionRequest;
import com.sequenceiq.provisioning.domain.AzureStack;
import com.sequenceiq.provisioning.domain.CloudPlatform;

@Component
public class AzureStackConverter extends AbstractConverter<ProvisionRequest, AzureStack> {

    @Override
    public ProvisionRequest convert(AzureStack entity) {
        ProvisionRequest azureStackJson = new ProvisionRequest();
        azureStackJson.setClusterName(entity.getName());
        azureStackJson.setCloudPlatform(CloudPlatform.AZURE);
        azureStackJson.setClusterSize(entity.getClusterSize());
        return azureStackJson;
    }

    @Override
    public AzureStack convert(ProvisionRequest json) {
        AzureStack azureStack = new AzureStack();
        azureStack.setName(json.getClusterName());
        azureStack.setClusterSize(json.getClusterSize());
        return azureStack;
    }
}
