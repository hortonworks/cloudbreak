package com.sequenceiq.freeipa.service.image;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Service
public class PreferredOsService {

    private static final String REDHAT8 = "redhat8";

    @Inject
    private EntitlementService entitlementService;

    @Value("${freeipa.image.catalog.default.os}")
    private String defaultOs;

    public String getPreferredOs() {
        return getPreferredOs(null);
    }

    public String getPreferredOs(String requestedOs) {
        if (StringUtils.isNotBlank(requestedOs)) {
            return requestedOs;
        } else if (entitlementService.isRhel8ImagePreferred(ThreadBasedUserCrnProvider.getAccountId())) {
            return REDHAT8;
        } else {
            return defaultOs;
        }
    }
}
