package com.sequenceiq.provisioning.service.aws;

import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.json.CredentialRequest;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.service.CredentialService;

@Service
public class AWSCredentialService implements CredentialService {

    @Override
    public void saveCredentials(User user, CredentialRequest credentialRequest) {

    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

}
