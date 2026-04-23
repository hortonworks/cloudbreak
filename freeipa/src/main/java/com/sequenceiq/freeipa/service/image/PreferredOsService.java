package com.sequenceiq.freeipa.service.image;

import static com.sequenceiq.common.model.OsType.getLatestOsType;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;

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
        if (mustUseLatestOsType(accountId)) {
            return getLatestOsType().getOs();
        } else if (StringUtils.isNotBlank(requestedOs)) {
            return getOsTypeBasedOnRequestedOsType(requestedOs, accountId);
        } else {
            return defaultOs;
        }
    }

    private String getOsTypeBasedOnRequestedOsType(String requestedOs, String accountId) {
        if (requestedLatestOs(requestedOs)) {
            if (notInternalUser() && entitlementService.isEntitledToUseOS(accountId, getLatestOsType())) {
                return requestedOs;
            } else {
                return defaultOs;
            }
        } else {
            return requestedOs;
        }
    }

    private boolean requestedLatestOs(String requestedOs) {
        return getLatestOsType().getOs().equalsIgnoreCase(requestedOs);
    }

    private boolean mustUseLatestOsType(String accountId) {
        return notInternalUser() && entitlementService.isEntitledToUseOS(accountId, getLatestOsType());
    }

    private boolean notInternalUser() {
        return !RegionAwareInternalCrnGeneratorUtil.isInternalCrn(ThreadBasedUserCrnProvider.getUserCrn());
    }
}
