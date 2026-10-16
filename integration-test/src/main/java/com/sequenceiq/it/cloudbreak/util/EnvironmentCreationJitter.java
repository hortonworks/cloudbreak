package com.sequenceiq.it.cloudbreak.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.log.Log;

/**
 * Spreads the first wave of environment create requests of a test JVM over time. Parallel E2E jobs start at the same moment against freshly
 * started environment-service pods, and a burst of concurrent creates can exceed the flow accept timeout there (CB-35055).
 * Only the creates requested within {@code windowSeconds} after the first one are delayed, so later tests of a long suite are not slowed down.
 */
@Component
public class EnvironmentCreationJitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCreationJitter.class);

    private final AtomicReference<Instant> firstRequest = new AtomicReference<>();

    private final long maxSeconds;

    private final long windowSeconds;

    private final Clock clock;

    @Autowired
    public EnvironmentCreationJitter(@Value("${integrationtest.environment.createJitter.maxSeconds:0}") long maxSeconds,
            @Value("${integrationtest.environment.createJitter.windowSeconds:120}") long windowSeconds) {
        this(maxSeconds, windowSeconds, Clock.systemUTC());
    }

    EnvironmentCreationJitter(long maxSeconds, long windowSeconds, Clock clock) {
        this.maxSeconds = maxSeconds;
        this.windowSeconds = windowSeconds;
        this.clock = clock;
    }

    /**
     * @return the applied delay in milliseconds, 0 if the jitter is disabled or its window is over
     */
    public long delayIfNeeded(String environmentName) {
        if (maxSeconds <= 0) {
            return 0L;
        }
        Instant now = clock.instant();
        firstRequest.compareAndSet(null, now);
        long secondsSinceFirstCreate = Duration.between(firstRequest.get(), now).getSeconds();
        if (secondsSinceFirstCreate >= windowSeconds) {
            return 0L;
        }
        long delayMillis = ThreadLocalRandom.current().nextLong(Duration.ofSeconds(maxSeconds).toMillis() + 1);
        Log.when(LOGGER, String.format("Delaying environment creation of '%s' by %d s to spread start-up load (jitter max %d s, %d s after first create)",
                environmentName, Duration.ofMillis(delayMillis).getSeconds(), maxSeconds, secondsSinceFirstCreate));
        CloudbreakUtil.sleep(delayMillis);
        return delayMillis;
    }
}
