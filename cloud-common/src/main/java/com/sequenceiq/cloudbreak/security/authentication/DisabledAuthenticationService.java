package com.sequenceiq.cloudbreak.security.authentication;

import org.springframework.security.oauth2.provider.OAuth2Authentication;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;

public class DisabledAuthenticationService implements AuthenticationService {

    @Override
    public CloudbreakUser getCloudbreakUser(OAuth2Authentication auth) {
        return new CloudbreakUser("disabledAuthenticationUserId",
                "disabledAuthenticationUserCrn",
                "disabledAuthenticationUsername",
                "disabledAuth@cloudera.com",
                "cloudera");
    }
}
