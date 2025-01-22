package com.sequenceiq.cloudbreak.auth.security.authentication;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

public class DisabledAuthenticationService implements AuthenticationService {

    private final DisabledAuthCbUserProvider disabledAuthCbUserProvider;

    public DisabledAuthenticationService(DisabledAuthCbUserProvider disabledAuthCbUserProvider) {
        this.disabledAuthCbUserProvider = disabledAuthCbUserProvider;
    }

    @Override
    public CloudbreakUser getCloudbreakUser(String userCrn) {
        return disabledAuthCbUserProvider.getCloudbreakUser();
    }
}
