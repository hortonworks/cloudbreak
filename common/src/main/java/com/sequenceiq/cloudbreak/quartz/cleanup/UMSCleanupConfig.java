package com.sequenceiq.cloudbreak.quartz.cleanup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UMSCleanupConfig {

    @Value("${umscleanup.enabled:false}")
    private boolean enabled;

    @Value("${umscleanup.quartz.cron.expression:0 0 0 ? * SUN}")
    private String cronExpression;

    @Value("${umscleanup.max.age.days:3}")
    private int maxAgeDays;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxAgeDays() {
        return maxAgeDays;
    }

    public void setMaxAgeDays(int maxAgeDays) {
        this.maxAgeDays = maxAgeDays;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
}
