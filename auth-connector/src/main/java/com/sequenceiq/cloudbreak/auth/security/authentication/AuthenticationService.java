package com.sequenceiq.cloudbreak.auth.security.authentication;

import org.springframework.security.core.Authentication;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

public interface AuthenticationService {

    CloudbreakUser getCloudbreakUser(Authentication auth);
}
