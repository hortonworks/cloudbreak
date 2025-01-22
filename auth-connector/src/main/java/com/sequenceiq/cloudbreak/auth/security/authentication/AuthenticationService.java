package com.sequenceiq.cloudbreak.auth.security.authentication;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

public interface AuthenticationService {

    CloudbreakUser getCloudbreakUser(String userCrn);
}
