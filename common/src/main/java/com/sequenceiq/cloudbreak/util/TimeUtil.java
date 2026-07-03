package com.sequenceiq.cloudbreak.util;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

@Component
public class TimeUtil {

    public static final int MILLISEC_MULTIPLIER = 1000;

    public static final int ONE_HOUR_IN_MINUTES = 60;

    private static final long THIRTY_DAYS = 30L;

    public Long getTimestampThatMonthsBeforeNow(int amountOfMonths) {
        return Instant.now().getEpochSecond() * MILLISEC_MULTIPLIER - Duration.ofDays(THIRTY_DAYS * amountOfMonths).toMillis();
    }

    public Long getTimestampThatDaysBeforeNow(int amountOfDays) {
        return Instant.now().getEpochSecond() * MILLISEC_MULTIPLIER - Duration.ofDays(amountOfDays).toMillis();
    }

    public Long getTimestampThatDaysAfterNow(int amountOfDays) {
        return Instant.now().getEpochSecond() * MILLISEC_MULTIPLIER + Duration.ofDays(amountOfDays).toMillis();
    }
}
