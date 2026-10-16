package com.sequenceiq.it.cloudbreak.util;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class EnvironmentCreationJitterTest {

    private static final String ENVIRONMENT_NAME = "env-under-test";

    private MutableClock clock;

    private MockedStatic<CloudbreakUtil> cloudbreakUtil;

    @BeforeMethod
    public void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-16T10:00:00Z"));
        cloudbreakUtil = mockStatic(CloudbreakUtil.class);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        cloudbreakUtil.close();
    }

    @Test
    public void testDisabledWhenMaxSecondsIsZero() {
        EnvironmentCreationJitter jitter = new EnvironmentCreationJitter(0, 120, clock);

        long delay = jitter.delayIfNeeded(ENVIRONMENT_NAME);

        assertEquals(delay, 0L);
        cloudbreakUtil.verify(() -> CloudbreakUtil.sleep(anyLong()), never());
    }

    @Test
    public void testFirstRequestIsDelayedWithinMaxSeconds() {
        EnvironmentCreationJitter jitter = new EnvironmentCreationJitter(45, 120, clock);

        long delay = jitter.delayIfNeeded(ENVIRONMENT_NAME);

        assertTrue(delay >= 0 && delay <= 45_000, "delay out of range: " + delay);
        cloudbreakUtil.verify(() -> CloudbreakUtil.sleep(delay));
    }

    @Test
    public void testRequestWithinWindowIsDelayed() {
        EnvironmentCreationJitter jitter = new EnvironmentCreationJitter(45, 120, clock);
        jitter.delayIfNeeded(ENVIRONMENT_NAME);
        clock.advance(Duration.ofSeconds(60));

        long delay = jitter.delayIfNeeded(ENVIRONMENT_NAME);

        assertTrue(delay >= 0 && delay <= 45_000, "delay out of range: " + delay);
        cloudbreakUtil.verify(() -> CloudbreakUtil.sleep(anyLong()), times(2));
    }

    @Test
    public void testRequestAfterWindowIsNotDelayed() {
        EnvironmentCreationJitter jitter = new EnvironmentCreationJitter(45, 120, clock);
        jitter.delayIfNeeded(ENVIRONMENT_NAME);
        clock.advance(Duration.ofSeconds(121));

        long delay = jitter.delayIfNeeded(ENVIRONMENT_NAME);

        assertEquals(delay, 0L);
        cloudbreakUtil.verify(() -> CloudbreakUtil.sleep(anyLong()), times(1));
    }

    /**
     * Clock whose time only moves when {@link #advance(Duration)} is called.
     */
    private static final class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant start) {
            this.instant = start;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
