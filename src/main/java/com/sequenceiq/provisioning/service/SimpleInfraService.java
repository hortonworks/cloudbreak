package com.sequenceiq.provisioning.service;

import java.util.HashSet;
import java.util.Set;
import java.util.UnknownFormatConversionException;

import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.json.CloudInstanceResult;
import com.sequenceiq.provisioning.controller.json.InfraRequest;
import com.sequenceiq.provisioning.converter.AwsInfraConverter;
import com.sequenceiq.provisioning.converter.AzureInfraConverter;
import com.sequenceiq.provisioning.domain.AwsInfra;
import com.sequenceiq.provisioning.domain.AzureInfra;
import com.sequenceiq.provisioning.domain.Infra;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.InfraRepository;

@Service
public class SimpleInfraService implements InfraService {

    @Autowired
    private InfraRepository infraRepository;

    @Autowired
    private AwsInfraConverter awsInfraConverter;

    @Autowired
    private AzureInfraConverter azureInfraConverter;

    @Override
    public Set<InfraRequest> getAll(User user) {
        Set<InfraRequest> result = new HashSet<>();
        result.addAll(awsInfraConverter.convertAllEntityToJson(user.getAwsInfraList()));
        result.addAll(azureInfraConverter.convertAllEntityToJson(user.getAzureInfraList()));
        return result;
    }

    @Override
    public InfraRequest get(Long id) {
        Infra one = infraRepository.findOne(id);
        if (one == null) {
            throw new EntityNotFoundException("Entity not exist with id: " + id);
        } else {
            switch (one.cloudPlatform()) {
            case AWS:
                return awsInfraConverter.convert((AwsInfra) one);
            case AZURE:
                return azureInfraConverter.convert((AzureInfra) one);
            default:
                throw new UnknownFormatConversionException("The cloudPlatform type not supported.");
            }
        }
    }

    @Override
    public CloudInstanceResult create(User user, InfraRequest infraRequest) {
        switch (infraRequest.getCloudPlatform()) {
        case AWS:
            Infra awsInfra = awsInfraConverter.convert(infraRequest);
            awsInfra.setUser(user);
            infraRepository.save(awsInfra);
            break;
        case AZURE:
            Infra azureInfra = azureInfraConverter.convert(infraRequest);
            azureInfra.setUser(user);
            infraRepository.save(azureInfra);
            break;
        default:
            throw new UnknownFormatConversionException("The cloudPlatform type not supported.");
        }
        return new CloudInstanceResult("OK");
    }

}
