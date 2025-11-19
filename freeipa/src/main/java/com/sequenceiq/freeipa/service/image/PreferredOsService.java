package com.sequenceiq.freeipa.service.image;

import static com.sequenceiq.common.model.OsType.RHEL9;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;
import com.sequenceiq.common.model.OsType;

@Service
public class PreferredOsService {

    @Value("${freeipa.image.catalog.default.os}")
    private String defaultOs;

    @Inject
    private EntitlementService entitlementService;

    public String getDefaultOs() {
        return getPreferredOs(null);
    }

    public String getPreferredOs(String requestedOs) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (mustUseRhel9(accountId)) {
            return RHEL9.getOs();
        } else if (requestedASpecificOsType(requestedOs)) {
            return getOsTypeBasedOnRequestedOsType(requestedOs, accountId);
        } else {
            return defaultOs;
        }
    }

    private String getOsTypeBasedOnRequestedOsType(String requestedOs, String accountId) {
        if (requestedRhel9(requestedOs)) {
            if (notInternalUser() && entitlementService.isEntitledToUseOS(accountId, RHEL9)) {
                return requestedOs;
            } else {
                return defaultOs;
            }
        } else {
            return requestedOs;
        }
    }

    private boolean requestedASpecificOsType(String requestedOs) {
        return StringUtils.isNotBlank(requestedOs);
    }

    private boolean requestedRhel9(String requestedOs) {
        return OsType.RHEL9.getOs().equalsIgnoreCase(requestedOs);
    }

    private boolean mustUseRhel9(String accountId) {
        return notInternalUser()
                && entitlementService.isEntitledToUseOS(accountId, RHEL9)
                && entitlementService.isRhel9ImagePreferred(accountId);
    }

    private boolean notInternalUser() {
        return !RegionAwareInternalCrnGeneratorUtil.isInternalCrn(ThreadBasedUserCrnProvider.getUserCrn());
    }
}
