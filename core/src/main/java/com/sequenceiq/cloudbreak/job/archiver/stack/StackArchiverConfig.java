package com.sequenceiq.cloudbreak.job.archiver.stack;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StackArchiverConfig {

    @Value("${archivestack.intervalseconds:60}")
    private int intervalInSeconds;

    @Value("${archivestack.retention.period.days:90}")
    private int retentionPeriodInDays;

    @Value("${archivestack.archiver.limit:30}")
    private int limitForStack;

    public int getIntervalInSeconds() {
        return intervalInSeconds;
    }

    public int getRetentionPeriodInDays() {
        return retentionPeriodInDays;
    }

    public int getLimitForStack() {
        return limitForStack;
    }
}
