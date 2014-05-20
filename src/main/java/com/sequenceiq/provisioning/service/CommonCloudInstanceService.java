package com.sequenceiq.provisioning.service;

import java.util.HashSet;
import java.util.Set;
import java.util.UnknownFormatConversionException;

import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.json.CloudInstanceRequest;
import com.sequenceiq.provisioning.converter.AwsCloudInstanceConverter;
import com.sequenceiq.provisioning.converter.AzureCloudInstanceConverter;
import com.sequenceiq.provisioning.domain.AwsCloudInstance;
import com.sequenceiq.provisioning.domain.AzureCloudInstance;
import com.sequenceiq.provisioning.domain.CloudInstance;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.AwsCloudInstanceRepository;
import com.sequenceiq.provisioning.repository.AzureCloudInstanceRepository;
import com.sequenceiq.provisioning.repository.CloudInstanceRepository;

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

    @Autowired
    private CloudInstanceRepository cloudInstanceRepository;

    public Set<CloudInstanceRequest> getAll(User user) {
        Set<CloudInstanceRequest> result = new HashSet<>();
        result.addAll(awsCloudInstanceConverter.convertAllEntityToJson(user.getAwsCloudInstanceList()));
        result.addAll(azureCloudInstanceConverter.convertAllEntityToJson(user.getAzureCloudInstanceList()));
        return result;
    }

    public CloudInstanceRequest get(Long id) {
        CloudInstance one = cloudInstanceRepository.findOne(id);
        if (one == null) {
            throw new EntityNotFoundException("Entity not exist with id: " + id);
        } else {
            switch (one.cloudPlatform()) {
                case AWS:
                    return awsCloudInstanceConverter.convert((AwsCloudInstance) one);
                case AZURE:
                    return azureCloudInstanceConverter.convert((AzureCloudInstance) one);
                default:
                    throw new UnknownFormatConversionException("The cloudPlatform type not supported.");
            }
        }
    }

}
