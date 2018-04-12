package com.sequenceiq.cloudbreak.converter.spi;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.domain.Credential;

@Component
public class CredentialToExtendedCloudCredentialConverter {

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    public ExtendedCloudCredential convert(Credential credential) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        return new ExtendedCloudCredential(cloudCredential, credential.cloudPlatform(), credential.getDescription(), credential.getOwner(),
                credential.getAccount(), credential.isPublicInAccount());
    }
}
