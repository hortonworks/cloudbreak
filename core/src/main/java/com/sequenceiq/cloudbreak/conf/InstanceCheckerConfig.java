package com.sequenceiq.cloudbreak.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("instancechecker")
public class InstanceCheckerConfig {

    private static final boolean INSTANCE_CHECKER_ENABLED_BY_DEFAULT = true;

    private static final int DEFAULT_INSTANCE_CHECKER_INTERVAL_IN_HOURS = 6;

    private static final int DEFAULT_INSTANCE_CHECKER_DELAY_IN_SECONDS = 7200;

    private boolean enabled = INSTANCE_CHECKER_ENABLED_BY_DEFAULT;

    private int instanceCheckerIntervalInHours = DEFAULT_INSTANCE_CHECKER_INTERVAL_IN_HOURS;

    private int instanceCheckerDelayInSeconds = DEFAULT_INSTANCE_CHECKER_DELAY_IN_SECONDS;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getInstanceCheckerIntervalInHours() {
        return instanceCheckerIntervalInHours;
    }

    public void setInstanceCheckerIntervalInHours(int instanceCheckerIntervalInHours) {
        this.instanceCheckerIntervalInHours = instanceCheckerIntervalInHours;
    }

    public int getInstanceCheckerDelayInSeconds() {
        return instanceCheckerDelayInSeconds;
    }

    public void setInstanceCheckerDelayInSeconds(int instanceCheckerDelayInSeconds) {
        this.instanceCheckerDelayInSeconds = instanceCheckerDelayInSeconds;
    }
}
