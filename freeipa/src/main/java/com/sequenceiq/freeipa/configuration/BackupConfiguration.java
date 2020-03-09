package com.sequenceiq.freeipa.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BackupConfiguration {
    private final boolean monthlyFullEnabled;

    private final boolean runInitialFullAfterInstall;

    private final boolean hourlyEnabled;

    public BackupConfiguration(
            @Value("${freeipa.backup.full.monthly:true}") boolean monthlyFullEnabled,
            @Value("${freeipa.backup.full.after_install:true}") boolean runInitialFullAfterInstall,
            @Value("${freeipa.backup.hourly.enabled:true}") boolean hourlyEnabled) {
        this.monthlyFullEnabled = monthlyFullEnabled;
        this.runInitialFullAfterInstall = runInitialFullAfterInstall;
        this.hourlyEnabled = hourlyEnabled;
    }

    public boolean isMonthlyFullEnabled() {
        return monthlyFullEnabled;
    }

    public boolean isRunInitialFullAfterInstall() {
        return runInitialFullAfterInstall;
    }

    public boolean isHourlyEnabled() {
        return hourlyEnabled;
    }
}
