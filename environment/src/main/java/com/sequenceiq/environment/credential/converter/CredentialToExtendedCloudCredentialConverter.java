package com.sequenceiq.environment.credential.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.environment.credential.domain.Credential;

@Component
public class CredentialToExtendedCloudCredentialConverter {

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    public ExtendedCloudCredential convert(Credential credential) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        CloudbreakUser cloudbreakUser = authenticatedUserService.getCbUser();
        return new ExtendedCloudCredential(cloudCredential, credential.getCloudPlatform(), credential.getDescription(),
                cloudbreakUser, "0", 0L);
    }
}
