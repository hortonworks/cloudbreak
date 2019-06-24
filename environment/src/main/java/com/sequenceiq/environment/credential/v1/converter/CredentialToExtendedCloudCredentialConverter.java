package com.sequenceiq.environment.credential.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.environment.credential.domain.Credential;

@Component
public class CredentialToExtendedCloudCredentialConverter {

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    private final ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    public CredentialToExtendedCloudCredentialConverter(CredentialToCloudCredentialConverter credentialToCloudCredentialConverter,
            ThreadBasedUserCrnProvider threadBasedUserCrnProvider) {
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        this.threadBasedUserCrnProvider = threadBasedUserCrnProvider;
    }

    public ExtendedCloudCredential convert(Credential credential) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        return new ExtendedCloudCredential(cloudCredential, credential.getCloudPlatform(), credential.getDescription(), userCrn, accountId);
    }
}
