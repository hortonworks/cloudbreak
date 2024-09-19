package com.sequenceiq.cloudbreak.sdx.cdl.service;


import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.ENABLE_CONTAINERIZED_DATALAKE;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxCommonService;

public abstract class AbstractCdlSdxService implements PlatformAwareSdxCommonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCdlSdxService.class);

    @Value("${sdx.cdl.enabled:false}")
    private boolean cdlEnabled;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public TargetPlatform targetPlatform() {
        return TargetPlatform.CDL;
    }

    @Override
    public boolean isPlatformEntitled(String accountId) {
        return entitlementService.isEntitledFor(accountId, ENABLE_CONTAINERIZED_DATALAKE);
    }

    public boolean isEnabled(String crn) {
        boolean enabled = cdlEnabled && isPlatformEntitled(Crn.safeFromString(crn).getAccountId());
        if (!cdlEnabled) {
            LOGGER.debug("CDL is not enabled. {} {}",
                    cdlEnabled, isPlatformEntitled(Crn.safeFromString(crn).getAccountId()));
        }
        return enabled;
    }

}