package com.sequenceiq.cloudbreak.job.archiver.stack;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StackArchiverConfig {

    @Value("${archivestack.intervalhours:24}")
    private int intervalInHours;

    @Value("${archivestack.retention.period.days:14}")
    private int retentionPeriodInDays;

    public int getIntervalInHours() {
        return intervalInHours;
    }

    public int getRetentionPeriodInDays() {
        return retentionPeriodInDays;
    }
}
