package com.sequenceiq.cloudbreak.template.utils;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Component
public class HostGroupUtils {

    @Inject
    private EntitlementService entitlementService;

    public boolean isEcsHostGroup(String hostGroupName) {
        return entitlementService.internalTenant(ThreadBasedUserCrnProvider.getAccountId()) && StringUtils.startsWithIgnoreCase(hostGroupName, "ecs_");
    }

    public boolean isNotEcsHostGroup(String hostGroupName) {
        return !isEcsHostGroup(hostGroupName);
    }
}
