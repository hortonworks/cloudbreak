package com.sequenceiq.provisioning.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.ProvisionRequest;
import com.sequenceiq.provisioning.domain.AwsStack;

@Component
public class AwsStackConverter extends AbstractConverter<ProvisionRequest, AwsStack> {

    @Override
    public ProvisionRequest convert(AwsStack entity) {
        ProvisionRequest awsStackJson = new ProvisionRequest();
        awsStackJson.setClusterName(entity.getName());
        awsStackJson.setClusterSize(entity.getClusterSize());
        return awsStackJson;
    }

    @Override
    public AwsStack convert(ProvisionRequest json) {
        AwsStack awsStack = new AwsStack();
        awsStack.setName(json.getClusterName());
        return awsStack;
    }
}
