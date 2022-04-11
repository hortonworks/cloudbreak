package com.sequenceiq.cloudbreak.saas.sdx;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

public abstract class AbstractSdxService<S> implements SdxService<S> {

    @Inject
    private EntitlementService entitlementService;

    @Override
    public EntitlementService getEntitlementService() {
        return entitlementService;
    }
}
