package com.sequenceiq.environment.environment.scheduled.archiver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvironmentArchiverConfig {

    @Value("${environment.archiver.intervalhours:24}")
    private int intervalInHours;

    @Value("${environment.archiver.retention.period.days:14}")
    private int retentionPeriodInDays;

    public int getIntervalInHours() {
        return intervalInHours;
    }

    public int getRetentionPeriodInDays() {
        return retentionPeriodInDays;
    }
}
