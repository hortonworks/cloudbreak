package com.sequenceiq.cloudbreak.service;

import javax.annotation.Nullable;

import org.springframework.stereotype.Service;

@Service
public class RestRequestThreadLocalService {

    private static final ThreadLocal<Long> REQUESTED_ORG_ID = new ThreadLocal<>();

    public void setRequestedOrgId(@Nullable Long orgId) {
        REQUESTED_ORG_ID.set(orgId);
    }

    @Nullable
    public Long getRequestedOrgId() {
        return REQUESTED_ORG_ID.get();
    }
}
