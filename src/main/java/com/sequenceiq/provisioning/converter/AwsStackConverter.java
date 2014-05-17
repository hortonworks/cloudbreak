package com.sequenceiq.provisioning.converter;


import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.domain.AwsStack;
import com.sequenceiq.provisioning.json.AwsStackJson;

@Component
public class AwsStackConverter extends AbstractConverter<AwsStackJson, AwsStack> {

    @Override
    public AwsStackJson convert(AwsStack entity) {
        AwsStackJson awsStackJson = new AwsStackJson();
        awsStackJson.setName(entity.getName());
        return awsStackJson;
    }

    @Override
    public AwsStack convert(AwsStackJson json) {
        AwsStack awsStack = new AwsStack();
        awsStack.setName(json.getName());
        return awsStack;
    }
}
