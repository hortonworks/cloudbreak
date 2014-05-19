package com.sequenceiq.provisioning.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.InfraRequest;
import com.sequenceiq.provisioning.domain.AwsInfra;
import com.sequenceiq.provisioning.domain.CloudPlatform;

@Component
public class AwsInfraConverter extends AbstractConverter<InfraRequest, AwsInfra> {

    @Override
    public InfraRequest convert(AwsInfra entity) {
        InfraRequest awsStackJson = new InfraRequest();
        awsStackJson.setClusterName(entity.getName());
        awsStackJson.setCloudPlatform(CloudPlatform.AWS);
        return awsStackJson;
    }

    @Override
    public AwsInfra convert(InfraRequest json) {
        AwsInfra awsInfra = new AwsInfra();
        awsInfra.setName(json.getClusterName());
        return awsInfra;
    }
}
