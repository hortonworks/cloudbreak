package com.sequenceiq.freeipa.sync.dynamicentitlement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DynamicEntitlementRefreshConfig {

    @Value("${dynamic-entitlement.entitlements:}")
    private List<String> watchedEntitlements;

    @Value("${dynamic-entitlement.intervalminutes:15}")
    private int intervalInMinutes;

    @Value("${dynamic-entitlement.enabled:true}")
    private boolean enabled;

    public int getIntervalInMinutes() {
        return intervalInMinutes;
    }

    public boolean isDynamicEntitlementEnabled() {
        return enabled;
    }

    public Set<String> getWatchedEntitlements() {
        return new HashSet<>(watchedEntitlements);
    }

}
