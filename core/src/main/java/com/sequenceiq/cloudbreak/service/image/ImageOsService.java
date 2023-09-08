package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.common.model.OsType.RHEL8;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;

@Component
public class ImageOsService {

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private EntitlementService entitlementService;

    @Value("${cb.image.catalog.default.os}")
    private String defaultOs;

    public boolean isSupported(String os) {
        if (!defaultOs.equalsIgnoreCase(os) && RHEL8.getOs().equalsIgnoreCase(os)) {
            return entitlementService.isRhel8ImageSupportEnabled(restRequestThreadLocalService.getAccountId());
        }
        return true;
    }

    public String getPreferredOs() {
        return getPreferredOs(null);
    }

    public String getPreferredOs(String requestedOs) {
        if (StringUtils.isNotBlank(requestedOs)) {
            return requestedOs;
        } else if (entitlementService.isRhel8ImagePreferred(restRequestThreadLocalService.getAccountId())) {
            return RHEL8.getOs();
        } else {
            return defaultOs;
        }
    }
}
