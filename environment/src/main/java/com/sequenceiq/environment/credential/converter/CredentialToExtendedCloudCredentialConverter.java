package com.sequenceiq.environment.credential.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.user.UserService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.environment.credential.Credential;

@Component
public class CredentialToExtendedCloudCredentialConverter {

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private UserService userService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    public ExtendedCloudCredential convert(Credential credential) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        CloudbreakUser cloudbreakUser = authenticatedUserService.getCbUser();
        User user = userService.getOrCreate(cloudbreakUser);
        return new ExtendedCloudCredential(cloudCredential, credential.getCloudPlatform(), credential.getDescription(),
                cloudbreakUser, user.getUserId(), credential.getWorkspace().getId());
    }
}
