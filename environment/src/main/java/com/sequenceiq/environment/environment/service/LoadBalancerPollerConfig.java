package com.sequenceiq.environment.environment.service;

import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.util.PollingConfig;

@Component
public class LoadBalancerPollerConfig {

    @Value("${env.loadbalancer.update.polling.maximum.seconds:7200}")
    private Integer maxTime;

    @Value("${env.loadbalancer.update.polling.sleep.time.seconds:30}")
    private Integer sleepTime;

    private PollingConfig config;

    @PostConstruct
    public void initPollingConfig() {
        this.config = PollingConfig.builder()
                .withStopPollingIfExceptionOccured(true)
                .withSleepTime(sleepTime)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(maxTime)
                .withTimeoutTimeUnit(TimeUnit.SECONDS)
                .build();
    }

    public PollingConfig getConfig() {
        return config;
    }
}
