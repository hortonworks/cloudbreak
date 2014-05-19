package com.sequenceiq.provisioning.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.CloudInstanceRequest;
import com.sequenceiq.provisioning.domain.AzureCloudInstance;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.repository.AzureInfraRepository;

@Component
public class AzureCloudConverter extends AbstractConverter<CloudInstanceRequest, AzureCloudInstance> {

    @Autowired
    private AzureInfraRepository azureInfraRepository;

    @Override
    public CloudInstanceRequest convert(AzureCloudInstance entity) {
        CloudInstanceRequest cloudInstanceRequest = new CloudInstanceRequest();
        cloudInstanceRequest.setInfraId(String.valueOf(entity.getAzureInfra().getId()));
        cloudInstanceRequest.setClusterSize(entity.getClusterSize());
        cloudInstanceRequest.setCloudPlatform(CloudPlatform.AWS);
        return cloudInstanceRequest;
    }

    @Override
    public AzureCloudInstance convert(CloudInstanceRequest json) {
        AzureCloudInstance azureCloudInstance = new AzureCloudInstance();
        azureCloudInstance.setClusterSize(json.getClusterSize());
        azureCloudInstance.setAzureInfra(azureInfraRepository.findOne(Long.valueOf(json.getInfraId())));
        return azureCloudInstance;
    }

}
