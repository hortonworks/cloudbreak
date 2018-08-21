package com.sequenceiq.cloudbreak.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;

@Service
public class RestRequestThreadLocalService {

    private static final ThreadLocal<Long> REQUESTED_ORG_ID = new ThreadLocal<>();

    private static final ThreadLocal<IdentityUser> IDENTITY_USER = new ThreadLocal<>();

    public void setRequestedOrgId(Long orgId) {
        REQUESTED_ORG_ID.set(orgId);
    }

    public Long getRequestedOrgId() {
        return REQUESTED_ORG_ID.get();
    }

    public void removeRequestedOrgId() {
        REQUESTED_ORG_ID.remove();
    }

    public void setIdentityUser(IdentityUser identityUser) {
        IDENTITY_USER.set(identityUser);
    }

    public IdentityUser getIdentityUser() {
        return IDENTITY_USER.get();
    }

    public void removeIdentityUser() {
        IDENTITY_USER.remove();
    }
}
