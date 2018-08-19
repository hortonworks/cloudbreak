package com.sequenceiq.cloudbreak.converter.spi;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
public class CredentialToExtendedCloudCredentialConverter {

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private UserService userService;

    public ExtendedCloudCredential convert(Credential credential) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        return new ExtendedCloudCredential(cloudCredential, credential.cloudPlatform(), credential.getDescription(), credential.getOwner(),
                credential.getAccount(), credential.isPublicInAccount(), identityUser,
                userService.getCurrentUser().getUserId(), credential.getOrganization().getId());
    }
}
