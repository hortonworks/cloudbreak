package com.sequenceiq.provisioning.service.aws;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.json.CredentialJson;
import com.sequenceiq.provisioning.controller.validation.RequiredAWSCredentialParam;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.UserRepository;
import com.sequenceiq.provisioning.service.CredentialService;

@Service
public class AWSCredentialService implements CredentialService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void saveCredentials(User user, CredentialJson credentialRequest) {
        User managedUser = userRepository.findByEmail(user.getEmail());
        managedUser.setRoleArn(credentialRequest.getParameters().get(RequiredAWSCredentialParam.ROLE_ARN.getName()));
        userRepository.save(managedUser);
    }

    @Override
    public CredentialJson retrieveCredentials(User user) {
        CredentialJson credentialJson = new CredentialJson();
        credentialJson.setCloudPlatform(CloudPlatform.AWS);
        Map<String, String> parameters = new HashMap<>();
        parameters.put(RequiredAWSCredentialParam.ROLE_ARN.getName(), user.getRoleArn());
        credentialJson.setParameters(parameters);
        return credentialJson;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}
