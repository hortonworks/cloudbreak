package com.sequenceiq.cloudbreak.converter.spi;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.workspace.model.User;

@Component
public class CredentialToExtendedCloudCredentialConverter {

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private UserService userService;

    @Inject
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    public ExtendedCloudCredential convert(Credential credential) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        CloudbreakUser cloudbreakUser = legacyRestRequestThreadLocalService.getCloudbreakUser();
        User user = null;
        if (cloudbreakUser != null) {
            user = userService.getOrCreate(cloudbreakUser);
        } else {
            user = userService.findByUserCrn(credential.getCreator()).orElse(null);
        }
        return new ExtendedCloudCredential(cloudCredential, credential.cloudPlatform(), credential.getDescription(), user.getUserCrn(),
                user.getTenant().getName());
    }
}
