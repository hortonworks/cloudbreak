package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimeUtilTest {

    private static final long THIRTY_DAYS = 30L;

    private static final long NINETY_DAYS = 90L;

    private static final int MILLISEC_MULTIPLIER = 1000;

    private TimeUtil underTest;

    @BeforeEach
    void setUp() {
        underTest = new TimeUtil();
    }

    @Test
    void testGetTimestampThatMonthsBeforeNow() {
        int amountOfMonths = 1;
        int thresholdInMs = 3000;
        Long now = Instant.now().getEpochSecond() * MILLISEC_MULTIPLIER - Duration.ofDays(THIRTY_DAYS * amountOfMonths).toMillis();

        Long result = underTest.getTimestampThatMonthsBeforeNow(amountOfMonths);

        assertTrue(result - now < thresholdInMs);
    }

}