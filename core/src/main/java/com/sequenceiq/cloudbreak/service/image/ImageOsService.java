package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;
import static com.sequenceiq.common.model.OsType.RHEL9;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Component
public class ImageOsService {

    @Inject
    private EntitlementService entitlementService;

    @Value("${cb.image.catalog.default.os}")
    private String defaultOs;

    public boolean isSupported(String os) {
        //since os is not mandatory in the request, we should return true if it's not present
        return os == null || CENTOS7.getOs().equalsIgnoreCase(os) || CENTOS7.getOsType().equalsIgnoreCase(os) ||
                RHEL8.getOs().equalsIgnoreCase(os) ||
                RHEL9.getOs().equalsIgnoreCase(os);
    }

    public String getDefaultOs() {
        return defaultOs;
    }

    public String getPreferredOs() {
        return getPreferredOs(null);
    }

    public String getPreferredOs(String requestedOs) {
        if (StringUtils.isNotBlank(requestedOs)) {
            return requestedOs;
        } else if (entitlementService.isRhel9ImagePreferred(ThreadBasedUserCrnProvider.getAccountId())) {
            return RHEL9.getOs();
        } else {
            return defaultOs;
        }
    }
}
