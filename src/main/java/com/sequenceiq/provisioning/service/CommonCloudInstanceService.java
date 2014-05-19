package com.sequenceiq.provisioning.service;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.provisioning.controller.json.CloudInstanceRequest;
import com.sequenceiq.provisioning.converter.AwsCloudInstanceConverter;
import com.sequenceiq.provisioning.converter.AzureCloudInstanceConverter;
import com.sequenceiq.provisioning.domain.AwsCloudInstance;
import com.sequenceiq.provisioning.domain.AzureCloudInstance;
import com.sequenceiq.provisioning.repository.AwsCloudInstanceRepository;
import com.sequenceiq.provisioning.repository.AzureCloudInstanceRepository;

@Service
public class CommonCloudInstanceService {

    @Autowired
    private AwsCloudInstanceRepository awsCloudInstanceRepository;

    @Autowired
    private AzureCloudInstanceRepository azureCloudInstanceRepository;

    @Autowired
    private AwsCloudInstanceConverter awsCloudInstanceConverter;

    @Autowired
    private AzureCloudInstanceConverter azureCloudInstanceConverter;

    public Set<CloudInstanceRequest> getAll() {
        Set<CloudInstanceRequest> result = new HashSet<>();
        result.addAll(awsCloudInstanceConverter.convertAllEntityToJson(Lists.newArrayList(awsCloudInstanceRepository.findAll())));
        result.addAll(azureCloudInstanceConverter.convertAllEntityToJson(Lists.newArrayList(azureCloudInstanceRepository.findAll())));
        return result;
    }

    public CloudInstanceRequest get(Long id) {
        AwsCloudInstance awsInstance = awsCloudInstanceRepository.findOne(id);
        if (awsInstance == null) {
            AzureCloudInstance azureInstance = azureCloudInstanceRepository.findOne(id);
            if (azureInstance == null) {
                throw new EntityNotFoundException("Entity not exist with id: " + id);
            } else {
                return azureCloudInstanceConverter.convert(azureInstance);
            }
        } else {
            return awsCloudInstanceConverter.convert(awsInstance);
        }
    }

}
