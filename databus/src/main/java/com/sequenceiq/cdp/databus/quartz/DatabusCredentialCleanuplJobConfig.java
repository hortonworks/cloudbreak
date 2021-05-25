package com.sequenceiq.cdp.databus.quartz;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DatabusCredentialCleanuplJobConfig {

    @Value("${databuscredential.cleanup.enabled:false}")
    private boolean enabled;

    @Value("${databuscredential.cleanup.quartz.cron.expression:0 0 5 * * ?}")
    private String cronExpression;

    @Value("${databuscredential.cleanup..max.age.days:2}")
    private int maxAgeDays;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public int getMaxAgeDays() {
        return maxAgeDays;
    }

    public void setMaxAgeDays(int maxAgeDays) {
        this.maxAgeDays = maxAgeDays;
    }
}
