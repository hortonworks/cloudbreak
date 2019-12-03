package com.sequenceiq.environment.credential.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.environment.credential.domain.Credential;

@Component
public class CredentialToExtendedCloudCredentialConverter {

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    public CredentialToExtendedCloudCredentialConverter(CredentialToCloudCredentialConverter credentialToCloudCredentialConverter) {
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
    }

    public ExtendedCloudCredential convert(Credential credential) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return new ExtendedCloudCredential(cloudCredential, credential.getCloudPlatform(), credential.getDescription(), userCrn, accountId);
    }
}
