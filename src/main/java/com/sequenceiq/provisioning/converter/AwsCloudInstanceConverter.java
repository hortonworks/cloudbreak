package com.sequenceiq.provisioning.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.CloudInstanceRequest;
import com.sequenceiq.provisioning.domain.AwsCloudInstance;
import com.sequenceiq.provisioning.repository.AwsInfraRepository;

@Component
public class AwsCloudInstanceConverter extends AbstractConverter<CloudInstanceRequest, AwsCloudInstance> {

    @Autowired
    private AwsInfraRepository awsInfraRepository;

    @Override
    public CloudInstanceRequest convert(AwsCloudInstance entity) {
        CloudInstanceRequest cloudInstanceRequest = new CloudInstanceRequest();
        cloudInstanceRequest.setInfraId(entity.getAwsInfra().getId());
        cloudInstanceRequest.setClusterSize(entity.getClusterSize());
        return cloudInstanceRequest;
    }

    @Override
    public AwsCloudInstance convert(CloudInstanceRequest json) {
        AwsCloudInstance awsCloudInstance = new AwsCloudInstance();
        awsCloudInstance.setClusterSize(json.getClusterSize());
        awsCloudInstance.setAwsInfra(awsInfraRepository.findOne(Long.valueOf(json.getInfraId())));
        return awsCloudInstance;
    }
}
