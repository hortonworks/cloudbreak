package com.sequenceiq.cloudbreak.job.dynamicentitlement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DynamicEntitlementRefreshConfig {

    @Value("${dynamic-entitlement.entitlements.cb:}")
    private List<String> cbEntitlements;

    @Value("${dynamic-entitlement.entitlements.cm:}")
    private List<String> cmEntitlements;

    @Value("${dynamic-entitlement.intervalminutes:15}")
    private int intervalInMinutes;

    @Value("${dynamic-entitlement.enabled:true}")
    private boolean enabled;

    public int getIntervalInMinutes() {
        return intervalInMinutes;
    }

    public List<String> getCbEntitlements() {
        return cbEntitlements;
    }

    public List<String> getCmEntitlements() {
        return cmEntitlements;
    }

    public boolean isDynamicEntitlementEnabled() {
        return enabled;
    }

    public Set<String> getWatchedEntitlements() {
        Set<String> result = new HashSet<>(cbEntitlements);
        result.addAll(cmEntitlements);
        return result;
    }

}
