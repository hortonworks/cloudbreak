package com.sequenceiq.provisioning.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.CloudInstanceRequest;
import com.sequenceiq.provisioning.controller.json.InfraRequest;
import com.sequenceiq.provisioning.domain.AwsCloudInstance;
import com.sequenceiq.provisioning.domain.AwsInfra;
import com.sequenceiq.provisioning.domain.AzureCloudInstance;
import com.sequenceiq.provisioning.domain.CloudPlatform;

@Component
public class AwsCloudConverter extends AbstractConverter<CloudInstanceRequest, AwsCloudInstance> {

    @Override
    public CloudInstanceRequest convert(AwsCloudInstance entity) {
     /*   InfraRequest awsStackJson = new InfraRequest();
        awsStackJson.setClusterName(entity.getName());
        awsStackJson.setCloudPlatform(CloudPlatform.AWS);*/
        return new CloudInstanceRequest();
    }

    @Override
    public AwsCloudInstance convert(CloudInstanceRequest json) {
     /*   AwsInfra awsInfra = new AwsInfra();
        awsInfra.setName(json.getClusterName());*/
        return new AwsCloudInstance();
    }
}
