package com.sequenceiq.notification.scheduled.cleanup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationCleanupConfig {

    @Value("${thunderheadnotification.cleanup.intervalhours:168}")
    private int intervalInHours;

    @Value("${thunderheadnotification.cleanup.delayhours:2}")
    private int maxDelayToStartInHours;

    @Value("${thunderheadnotification.cleanup.enabled:true}")
    private boolean enabled;

    public int getIntervalInHours() {
        return intervalInHours;
    }

    public int getMaxDelayToStartInHours() {
        return maxDelayToStartInHours;
    }

    public boolean enabled() {
        return enabled;
    }
}
