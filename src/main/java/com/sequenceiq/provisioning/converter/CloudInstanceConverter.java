package com.sequenceiq.provisioning.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.CloudInstanceRequest;
import com.sequenceiq.provisioning.domain.CloudInstance;
import com.sequenceiq.provisioning.repository.InfraRepository;

@Component
public class CloudInstanceConverter extends AbstractConverter<CloudInstanceRequest, CloudInstance> {

    @Autowired
    private InfraRepository infraRepository;

    @Override
    public CloudInstanceRequest convert(CloudInstance entity) {
        CloudInstanceRequest cloudInstanceRequest = new CloudInstanceRequest();
        cloudInstanceRequest.setInfraId(entity.getInfra().getId());
        cloudInstanceRequest.setClusterSize(entity.getClusterSize());
        return cloudInstanceRequest;
    }

    @Override
    public CloudInstance convert(CloudInstanceRequest json) {
        CloudInstance cloudInstance = new CloudInstance();
        cloudInstance.setClusterSize(json.getClusterSize());
        cloudInstance.setInfra(infraRepository.findOne(Long.valueOf(json.getInfraId())));
        return cloudInstance;
    }
}
