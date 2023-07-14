package com.sequenceiq.cloudbreak.core.flow2.externaldatabase;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.externaldatabase.PollingConfig;

@Component
public class ExternalDatabasePollingConfig {

    private static final int SLEEP_TIME_IN_SEC_FOR_DB_POLLING = 10;

    private static final int DURATION_IN_MINUTES_FOR_DB_POLLING = 60;

    private PollingConfig config;

    public ExternalDatabasePollingConfig() {
        this.config = PollingConfig.builder()
                .withSleepTime(SLEEP_TIME_IN_SEC_FOR_DB_POLLING)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(DURATION_IN_MINUTES_FOR_DB_POLLING)
                .withTimeoutTimeUnit(TimeUnit.MINUTES)
                .withStopPollingIfExceptionOccured(false)
                .build();
    }

    public PollingConfig getConfig() {
        return config;
    }

}
