package com.sequenceiq.cloudbreak.security.authentication;

import org.springframework.security.oauth2.provider.OAuth2Authentication;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;

public interface AuthenticationService {

    CloudbreakUser getCloudbreakUser(OAuth2Authentication auth);
}
