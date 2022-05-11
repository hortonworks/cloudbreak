package com.sequenceiq.datalake.job;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RollForwardConfig {

    @Value("${sdx.rollforward.interval.minutes:1}")
    private int intervalInMinutes;

    public int getIntervalInMinutes() {
        return intervalInMinutes;
    }
}
