package com.sequenceiq.notification.config;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

@Component
public class NotificationConfig {

    @Inject
    private EntitlementService entitlementService;

    @Value("${thunderheadnotification.enabled:true}")
    private boolean enabled;

    @Value("${thunderheadnotification.batchSize:1000}")
    private int batchSize;

    public int getBatchSize() {
        return batchSize;
    }

    public boolean isEnabled(Crn resourceCrn) {
        return enabled && entitlementService.isCdpCbNotificationSendingEnabled(resourceCrn.getAccountId());
    }

    public boolean isEnabledByAccountID(String acountId) {
        return enabled && entitlementService.isCdpCbNotificationSendingEnabled(acountId);
    }
}
