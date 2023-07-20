package com.sequenceiq.freeipa.service.image;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Service
public class SupportedOsService {

    private static final String REDHAT8 = "redhat8";

    @Inject
    private EntitlementService entitlementService;

    @Value("${freeipa.image.catalog.default.os}")
    private String defaultOs;

    public boolean isSupported(String os) {
        if (!defaultOs.equalsIgnoreCase(os) && REDHAT8.equalsIgnoreCase(os)) {
            return entitlementService.isRhel8ImageSupportEnabled(ThreadBasedUserCrnProvider.getAccountId());
        }
        return true;
    }

    public boolean isRhel8Supported() {
        return isSupported(REDHAT8);
    }
}
