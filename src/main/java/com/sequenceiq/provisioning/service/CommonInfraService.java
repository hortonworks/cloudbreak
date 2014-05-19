package com.sequenceiq.provisioning.service;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.json.InfraRequest;
import com.sequenceiq.provisioning.converter.AwsInfraConverter;
import com.sequenceiq.provisioning.converter.AzureInfraConverter;
import com.sequenceiq.provisioning.domain.AwsInfra;
import com.sequenceiq.provisioning.domain.AzureInfra;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.AwsInfraRepository;
import com.sequenceiq.provisioning.repository.AzureInfraRepository;
import com.sequenceiq.provisioning.repository.UserRepository;

@Service
public class CommonInfraService {

    @Autowired
    private AwsInfraRepository awsInfraRepository;

    @Autowired
    private AzureInfraRepository azureInfraRepository;

    @Autowired
    private AwsInfraConverter awsInfraConverter;

    @Autowired
    private AzureInfraConverter azureInfraConverter;

    @Autowired
    private UserRepository userRepository;

    public Set<InfraRequest> getAll(User user) {
        Set<InfraRequest> result = new HashSet<>();
        result.addAll(awsInfraConverter.convertAllEntityToJson(user.getAwsInfraList()));
        result.addAll(azureInfraConverter.convertAllEntityToJson(user.getAzureInfraList()));
        return result;
    }

    public InfraRequest get(Long id) {
        AwsInfra awsInfra = awsInfraRepository.findOne(id);
        if (awsInfra == null) {
            AzureInfra azureInfra = azureInfraRepository.findOne(id);
            if (azureInfra == null) {
                throw new EntityNotFoundException("Entity not exist with id: " + id);
            } else {
                return azureInfraConverter.convert(azureInfra);
            }
        } else {
            return awsInfraConverter.convert(awsInfra);
        }
    }

}
