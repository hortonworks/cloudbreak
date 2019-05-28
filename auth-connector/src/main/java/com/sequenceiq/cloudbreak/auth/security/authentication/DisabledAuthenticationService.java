package com.sequenceiq.cloudbreak.auth.security.authentication;

import org.springframework.security.oauth2.provider.OAuth2Authentication;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

public class DisabledAuthenticationService implements AuthenticationService {

    private final DisabledAuthCbUserProvider disabledAuthCbUserProvider;

    public DisabledAuthenticationService(DisabledAuthCbUserProvider disabledAuthCbUserProvider) {
        this.disabledAuthCbUserProvider = disabledAuthCbUserProvider;
    }

    @Override
    public CloudbreakUser getCloudbreakUser(OAuth2Authentication auth) {
        return disabledAuthCbUserProvider.getCloudbreakUser();
    }
}
