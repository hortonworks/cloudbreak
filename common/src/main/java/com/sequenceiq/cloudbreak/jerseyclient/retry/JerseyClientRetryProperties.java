package com.sequenceiq.cloudbreak.jerseyclient.retry;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("jersey.client.retry")
public class JerseyClientRetryProperties {

    private static final Duration DEFAULT_INITIAL_BACKOFF_DURATION = Duration.ofSeconds(3);

    private static final Duration DEFAULT_MAX_BACKOFF_DURATION = Duration.ofSeconds(60);

    private static final double DEFAULT_MULTIPLIER = 2.0;

    private static final Duration QUARTZ_INITIAL_BACKOFF_DURATION = Duration.ofSeconds(1);

    private static final Duration QUARTZ_MAX_BACKOFF_DURATION = Duration.ofSeconds(5);

    private Duration defaultInitialBackoffDuration = DEFAULT_INITIAL_BACKOFF_DURATION;

    private Duration defaultMaxBackoffDuration = DEFAULT_MAX_BACKOFF_DURATION;

    private double defaultMultiplier = DEFAULT_MULTIPLIER;

    private Duration quartzInitialBackoffDuration = QUARTZ_INITIAL_BACKOFF_DURATION;

    private Duration quartzMaxBackoffDuration = QUARTZ_MAX_BACKOFF_DURATION;

    private double quartzMultiplier = DEFAULT_MULTIPLIER;

    public Duration getDefaultInitialBackoffDuration() {
        return defaultInitialBackoffDuration;
    }

    public void setDefaultInitialBackoffDuration(Duration defaultInitialBackoffDuration) {
        this.defaultInitialBackoffDuration = defaultInitialBackoffDuration;
    }

    public Duration getDefaultMaxBackoffDuration() {
        return defaultMaxBackoffDuration;
    }

    public void setDefaultMaxBackoffDuration(Duration defaultMaxBackoffDuration) {
        this.defaultMaxBackoffDuration = defaultMaxBackoffDuration;
    }

    public double getDefaultMultiplier() {
        return defaultMultiplier;
    }

    public void setDefaultMultiplier(double defaultMultiplier) {
        this.defaultMultiplier = defaultMultiplier;
    }

    public Duration getQuartzInitialBackoffDuration() {
        return quartzInitialBackoffDuration;
    }

    public void setQuartzInitialBackoffDuration(Duration quartzInitialBackoffDuration) {
        this.quartzInitialBackoffDuration = quartzInitialBackoffDuration;
    }

    public Duration getQuartzMaxBackoffDuration() {
        return quartzMaxBackoffDuration;
    }

    public void setQuartzMaxBackoffDuration(Duration quartzMaxBackoffDuration) {
        this.quartzMaxBackoffDuration = quartzMaxBackoffDuration;
    }

    public double getQuartzMultiplier() {
        return quartzMultiplier;
    }

    public void setQuartzMultiplier(double quartzMultiplier) {
        this.quartzMultiplier = quartzMultiplier;
    }
}
