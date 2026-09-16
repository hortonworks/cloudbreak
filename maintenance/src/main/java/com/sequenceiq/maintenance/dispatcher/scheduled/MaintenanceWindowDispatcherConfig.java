package com.sequenceiq.maintenance.dispatcher.scheduled;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "maintenance.dispatcher")
public class MaintenanceWindowDispatcherConfig {

    private static final int DEFAULT_INTERVAL_IN_MINUTES = 15;

    private boolean enabled;

    private int intervalInMinutes = DEFAULT_INTERVAL_IN_MINUTES;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getIntervalInMinutes() {
        return intervalInMinutes;
    }

    public void setIntervalInMinutes(int intervalInMinutes) {
        this.intervalInMinutes = intervalInMinutes;
    }
}
