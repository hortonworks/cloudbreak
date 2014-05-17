package com.sequenceiq.provisioning.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.ProvisionRequest;
import com.sequenceiq.provisioning.domain.AzureStack;

@Component
public class AzureStackConverter extends AbstractConverter<ProvisionRequest, AzureStack> {

    @Override
    public ProvisionRequest convert(AzureStack entity) {
        ProvisionRequest azureStackJson = new ProvisionRequest();
        azureStackJson.setClusterName(entity.getName());
        return azureStackJson;
    }

    @Override
    public AzureStack convert(ProvisionRequest json) {
        AzureStack azureStack = new AzureStack();
        azureStack.setName(json.getClusterName());
        return azureStack;
    }
}
