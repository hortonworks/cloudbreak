package com.sequenceiq.provisioning.service.aws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.json.CloudInstanceResult;
import com.sequenceiq.provisioning.controller.json.InfraRequest;
import com.sequenceiq.provisioning.converter.AwsInfraConverter;
import com.sequenceiq.provisioning.domain.AwsInfra;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.AwsInfraRepository;
import com.sequenceiq.provisioning.repository.UserRepository;
import com.sequenceiq.provisioning.service.InfraService;

@Service
public class AwsInfraService implements InfraService {

    private static final String OK_STATUS = "ok";

    @Autowired
    private AwsInfraConverter awsInfraConverter;

    @Autowired
    private AwsInfraRepository awsInfraRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public CloudInstanceResult createInfra(User user, InfraRequest infraRequest) {
        AwsInfra convert = awsInfraConverter.convert(infraRequest);
        convert.setUser(user);
        user.getAwsInfraList().add(convert);
        userRepository.save(user);
        return new CloudInstanceResult(OK_STATUS);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}
