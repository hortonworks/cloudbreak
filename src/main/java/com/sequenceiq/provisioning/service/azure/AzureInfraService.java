package com.sequenceiq.provisioning.service.azure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.json.CloudInstanceResult;
import com.sequenceiq.provisioning.controller.json.InfraRequest;
import com.sequenceiq.provisioning.converter.AzureInfraConverter;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.AzureInfraRepository;
import com.sequenceiq.provisioning.repository.UserRepository;
import com.sequenceiq.provisioning.service.InfraService;

@Service
public class AzureInfraService implements InfraService {

    private static final String OK_STATUS = "ok";

    @Autowired
    private AzureInfraConverter azureInfraConverter;

    @Autowired
    private AzureInfraRepository azureInfraRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public CloudInstanceResult createInfra(User user, InfraRequest infraRequest) {
        user.getAzureInfraList().add(azureInfraConverter.convert(infraRequest));
        userRepository.save(user);
        return new CloudInstanceResult(OK_STATUS);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
