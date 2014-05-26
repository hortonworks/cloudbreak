package com.sequenceiq.provisioning.service.aws;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.json.CredentialJson;
import com.sequenceiq.provisioning.converter.AwsCredentialConverter;
import com.sequenceiq.provisioning.domain.AwsCredential;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.AwsCredentialRepository;
import com.sequenceiq.provisioning.repository.UserRepository;
import com.sequenceiq.provisioning.service.CredentialService;

@Service
public class AWSCredentialService implements CredentialService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AwsCredentialRepository awsCredentialRepository;

    @Autowired
    private AwsCredentialConverter awsCredentialConverter;

    @Override
    public void saveCredentials(User user, CredentialJson credentialRequest) {
        AwsCredential awsCredential = awsCredentialConverter.convert(credentialRequest);
        awsCredential.setUser(user);
        awsCredentialRepository.save(awsCredential);
    }

    @Override
    public Set<CredentialJson> retrieveCredentials(User user) {
        return awsCredentialConverter.convertAllEntityToJson(user.getAwsCredentials());
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}
