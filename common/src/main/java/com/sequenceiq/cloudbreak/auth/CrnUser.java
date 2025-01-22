package com.sequenceiq.cloudbreak.auth;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

public class CrnUser extends CloudbreakUser {

    public CrnUser(String userId, String userCrn, String username, String email, String tenant, String role) {
        super(userId, userCrn, username, email, tenant);
    }
}
