package com.sequenceiq.cloudbreak.converter.spi;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.User;

@Component
public class CredentialToExtendedCloudCredentialConverter {

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private UserService userService;

    @Inject
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Inject
    private EntitlementService entitlementService;

    public ExtendedCloudCredential convert(Credential credential, Optional<User> optionalUser) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        User user = null;
        if (optionalUser.isPresent()) {
            user = optionalUser.orElse(null);
        } else {
            CloudbreakUser cloudbreakUser = legacyRestRequestThreadLocalService.getCloudbreakUser();
            if (cloudbreakUser != null) {
                user = userService.getOrCreate(cloudbreakUser);
            } else {
                Crn crn = Crn.fromString(credential.getCreator());
                user = userService.getByUserIdAndTenant(crn.getUserId(), crn.getAccountId()).orElse(null);
            }
        }
        if (user == null) {
            throw new IllegalStateException("The user is not available for the credential: " + credential.getCreator());
        }
        return new ExtendedCloudCredential(
                cloudCredential,
                credential.cloudPlatform(),
                credential.getDescription(),
                user.getUserCrn(),
                user.getTenant().getName(),
                entitlementService.getEntitlements(credential.getAccount()));
    }

    public ExtendedCloudCredential convert(Credential credential) {
        return convert(credential, Optional.empty());
    }
}
