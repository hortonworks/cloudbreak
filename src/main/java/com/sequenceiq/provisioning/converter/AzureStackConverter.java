package com.sequenceiq.provisioning.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.domain.AzureStack;
import com.sequenceiq.provisioning.json.AzureStackJson;

@Component
public class AzureStackConverter extends AbstractConverter<AzureStackJson, AzureStack> {

    @Override
    public AzureStackJson convert(AzureStack entity) {
        AzureStackJson azureStackJson = new AzureStackJson();
        azureStackJson.setName(entity.getName());
        return azureStackJson;
    }

    @Override
    public AzureStack convert(AzureStackJson json) {
        AzureStack azureStack = new AzureStack();
        azureStack.setName(json.getName());
        return azureStack;
    }
}
