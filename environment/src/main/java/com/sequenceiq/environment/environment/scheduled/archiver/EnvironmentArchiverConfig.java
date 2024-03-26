package com.sequenceiq.environment.environment.scheduled.archiver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvironmentArchiverConfig {

    @Value("${environment.archiver.intervalhours:1}")
    private int intervalInHours;

    @Value("${environment.archiver.retention.period.days:90}")
    private int retentionPeriodInDays;

    @Value("${environment.archiver.limit:500}")
    private int limitForEnvironment;

    public int getIntervalInHours() {
        return intervalInHours;
    }

    public int getRetentionPeriodInDays() {
        return retentionPeriodInDays;
    }

    public int getLimitForEnvironment() {
        return limitForEnvironment;
    }
}
