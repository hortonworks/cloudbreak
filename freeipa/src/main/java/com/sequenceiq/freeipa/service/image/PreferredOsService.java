package com.sequenceiq.freeipa.service.image;

import static com.sequenceiq.common.model.OsType.RHEL8;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Service
public class PreferredOsService {

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
            return RHEL8.getOs();
        } else {
            return defaultOs;
        }
    }
}
