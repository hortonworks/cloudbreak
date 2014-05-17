package com.sequenceiq.provisioning.service;

import com.sequenceiq.provisioning.controller.json.CredentialRequest;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;

public interface CredentialService {

    void saveCredentials(User user, CredentialRequest credentialRequest);

    CloudPlatform getCloudPlatform();

}
