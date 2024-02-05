package com.sequenceiq.cloudbreak.util;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

@Component
public class TimeUtil {

    private static final long THIRTY_DAYS = 30L;

    private static final long NINETY_DAYS = 90L;

    private static final int MILLISEC_MULTIPLIER = 1000;

    public Long getTimestampThatMonthsBeforeNow(int amountOfMonths) {
        return Instant.now().getEpochSecond() * MILLISEC_MULTIPLIER - Duration.ofDays(THIRTY_DAYS * amountOfMonths).toMillis();
    }

    public Long getTimestampThatDaysBeforeNow(int amountOfDays) {
        return Instant.now().getEpochSecond() * MILLISEC_MULTIPLIER - Duration.ofDays(amountOfDays).toMillis();
    }
}
