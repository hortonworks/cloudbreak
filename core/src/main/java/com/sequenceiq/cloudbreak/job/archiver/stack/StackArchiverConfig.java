package com.sequenceiq.cloudbreak.job.archiver.stack;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StackArchiverConfig {

    @Value("${archivestack.intervalhours:4}")
    private int intervalInHours;

    @Value("${archivestack.retention.period.days:14}")
    private int retentionPeriodInDays;

    @Value("${archivestack.archiver.limit:500}")
    private int limitForStack;

    public int getIntervalInHours() {
        return intervalInHours;
    }

    public int getRetentionPeriodInDays() {
        return retentionPeriodInDays;
    }

    public int getLimitForStack() {
        return limitForStack;
    }
}
