package com.sequenceiq.provisioning.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.ProvisionRequest;
import com.sequenceiq.provisioning.domain.AwsStack;

@Component
public class AwsStackConverter extends AbstractConverter<ProvisionRequest, AwsStack> {

    @Override
    public ProvisionRequest convert(AwsStack entity) {
        ProvisionRequest azureStackJson = new ProvisionRequest();
        azureStackJson.setClusterName(entity.getName());
        return azureStackJson;
    }

    @Override
    public AwsStack convert(ProvisionRequest json) {
        AwsStack awsStack = new AwsStack();
        awsStack.setName(json.getClusterName());
        return awsStack;
    }
}
