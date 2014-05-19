package com.sequenceiq.provisioning.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.CloudInstanceRequest;
import com.sequenceiq.provisioning.domain.AwsCloudInstance;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.repository.AwsInfraRepository;

@Component
public class AwsCloudConverter extends AbstractConverter<CloudInstanceRequest, AwsCloudInstance> {

    @Autowired
    private AwsInfraRepository awsInfraRepository;

    @Override
    public CloudInstanceRequest convert(AwsCloudInstance entity) {
        CloudInstanceRequest cloudInstanceRequest = new CloudInstanceRequest();
        cloudInstanceRequest.setInfraId(String.valueOf(entity.getAwsInfra().getId()));
        cloudInstanceRequest.setClusterSize(entity.getClusterSize());
        cloudInstanceRequest.setCloudPlatform(CloudPlatform.AWS);
        return cloudInstanceRequest;
    }

    @Override
    public AwsCloudInstance convert(CloudInstanceRequest json) {
        AwsCloudInstance awsCloudInstance = new AwsCloudInstance();
        awsCloudInstance.setClusterSize(json.getClusterSize());
        awsCloudInstance.setAwsInfra(awsInfraRepository.findOne(Long.valueOf(json.getInfraId())));
        return new AwsCloudInstance();
    }
}
