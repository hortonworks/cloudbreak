package com.sequenceiq.freeipa.converter.cloud;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.util.CrnService;

@Component
public class CredentialToExtendedCloudCredentialConverter {

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private CrnService crnService;

    public ExtendedCloudCredential convert(Credential credential) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        return new ExtendedCloudCredential(cloudCredential, credential.getCloudPlatform(), "", crnService.getCurrentUserId(),
                crnService.getCurrentAccountId());
    }
}
