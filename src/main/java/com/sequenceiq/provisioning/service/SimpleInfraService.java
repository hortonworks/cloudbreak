package com.sequenceiq.provisioning.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UnknownFormatConversionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.BadRequestException;
import com.sequenceiq.provisioning.controller.NotFoundException;
import com.sequenceiq.provisioning.controller.json.InfraJson;
import com.sequenceiq.provisioning.converter.AwsInfraConverter;
import com.sequenceiq.provisioning.converter.AzureInfraConverter;
import com.sequenceiq.provisioning.domain.AwsInfra;
import com.sequenceiq.provisioning.domain.AzureInfra;
import com.sequenceiq.provisioning.domain.CloudInstance;
import com.sequenceiq.provisioning.domain.Infra;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.CloudInstanceRepository;
import com.sequenceiq.provisioning.repository.InfraRepository;

@Service
public class SimpleInfraService implements InfraService {

    @Autowired
    private InfraRepository infraRepository;

    @Autowired
    private AwsInfraConverter awsInfraConverter;

    @Autowired
    private AzureInfraConverter azureInfraConverter;

    @Autowired
    private CloudInstanceRepository cloudInstanceRepository;

    @Override
    public Set<InfraJson> getAll(User user) {
        Set<InfraJson> result = new HashSet<>();
        result.addAll(awsInfraConverter.convertAllEntityToJson(user.getAwsInfras()));
        result.addAll(azureInfraConverter.convertAllEntityToJson(user.getAzureInfras()));
        return result;
    }

    @Override
    public InfraJson get(Long id) {
        Infra infra = infraRepository.findOne(id);
        if (infra == null) {
            throw new NotFoundException(String.format("Infrastructure '%s' not found.", id));
        } else {
            switch (infra.cloudPlatform()) {
            case AWS:
                return awsInfraConverter.convert((AwsInfra) infra);
            case AZURE:
                return azureInfraConverter.convert((AzureInfra) infra);
            default:
                throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", infra.cloudPlatform()));
            }
        }
    }

    @Override
    public void create(User user, InfraJson infraRequest) {
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
            throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", infraRequest.getCloudPlatform()));
        }
    }

    @Override
    public void delete(Long id) {
        Infra infra = infraRepository.findOne(id);
        if (infra == null){
            throw new NotFoundException(String.format("Infrastructure '%s' not found.", id));
        }
        List<CloudInstance> allCloudForInfra = getAllCloudForInfra(id);
        if (allCloudForInfra.size() == 0) {
            infraRepository.delete(infra);
        } else {
            throw new BadRequestException(String.format("Infrastructure '%s' has some cloud dependency please remove clouds before the deletion.", id));
        }
    }

    private List<CloudInstance> getAllCloudForInfra(Long id) {
        return cloudInstanceRepository.findAllCloudForInfra(id);
    }

}
