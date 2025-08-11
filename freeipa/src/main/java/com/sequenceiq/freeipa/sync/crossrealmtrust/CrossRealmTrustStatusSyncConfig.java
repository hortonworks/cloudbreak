package com.sequenceiq.freeipa.sync.crossrealmtrust;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CrossRealmTrustStatusSyncConfig {

    @Value("${cross-realm-trust.status-sync.intervalminutes:3}")
    private int intervalInMinutes;

    @Value("${cross-realm-trust.status-sync.enabled:false}")
    private boolean enabled;

    public int getIntervalInMinutes() {
        return intervalInMinutes;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
