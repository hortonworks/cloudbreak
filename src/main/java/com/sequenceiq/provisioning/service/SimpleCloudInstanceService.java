package com.sequenceiq.provisioning.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.NotFoundException;
import com.sequenceiq.provisioning.controller.json.CloudInstanceJson;
import com.sequenceiq.provisioning.controller.json.CloudInstanceResult;
import com.sequenceiq.provisioning.converter.CloudInstanceConverter;
import com.sequenceiq.provisioning.domain.CloudInstance;
import com.sequenceiq.provisioning.domain.CloudInstanceDescription;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.Infra;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.CloudInstanceRepository;
import com.sequenceiq.provisioning.repository.InfraRepository;

@Service
public class SimpleCloudInstanceService implements CloudInstanceService {

    @Autowired
    private CloudInstanceConverter cloudInstanceConverter;

    @Autowired
    private CloudInstanceRepository cloudInstanceRepository;

    @Autowired
    private InfraRepository infraRepository;

    @Resource
    private Map<CloudPlatform, ProvisionService> provisionServices;

    @Override
    public Set<CloudInstanceJson> getAll(User user) {
        Set<CloudInstanceJson> result = new HashSet<>();
        result.addAll(cloudInstanceConverter.convertAllEntityToJson(user.getCloudInstances()));
        return result;
    }

    @Override
    public CloudInstanceJson get(User user, Long id) {
        CloudInstance cloudInstance = cloudInstanceRepository.findOne(id);
        if (cloudInstance == null) {
            throw new NotFoundException(String.format("CloudInstance '%s' not found", id));
        } else {
            CloudPlatform cp = cloudInstance.getInfra().cloudPlatform();
            CloudInstanceDescription description = provisionServices.get(cp).describeCloudInstance(user, cloudInstance);
            return cloudInstanceConverter.convert(cloudInstance, description);
        }
    }

    @Override
    public CloudInstanceResult create(User user, CloudInstanceJson cloudInstanceRequest) {
        Infra infra = infraRepository.findOne(cloudInstanceRequest.getInfraId());
        if (infra == null) {
            throw new EntityNotFoundException(String.format("Infrastructure '%s' not found", cloudInstanceRequest.getInfraId()));
        }
        CloudInstance cloudInstance = cloudInstanceConverter.convert(cloudInstanceRequest);
        cloudInstance.setUser(user);
        cloudInstanceRepository.save(cloudInstance);

        ProvisionService provisionService = provisionServices.get(infra.cloudPlatform());
        return provisionService.createCloudInstance(user, cloudInstance);
    }

    @Override
    public void delete(Long id) {
        CloudInstance cloudInstance = cloudInstanceRepository.findOne(id);
        if (cloudInstance == null) {
            throw new NotFoundException(String.format("CloudInstance '%s' not found.", id));
        }
        cloudInstanceRepository.delete(cloudInstance);
    }

}
