package com.sequenceiq.cloudbreak.authentication;

import org.springframework.security.oauth2.provider.OAuth2Authentication;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

public interface AuthenticationService {

    CloudbreakUser getCloudbreakUser(OAuth2Authentication auth);
}
