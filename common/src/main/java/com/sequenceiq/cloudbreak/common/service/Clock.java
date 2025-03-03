package com.sequenceiq.cloudbreak.common.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Date;

import org.springframework.stereotype.Service;

@Service
public class Clock {

    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    public Instant getCurrentInstant() {
        return Instant.now();
    }

    public Instant nowMinus(TemporalAmount amount) {
        return Instant.now().minus(amount);
    }

    public LocalDateTime getCurrentLocalDateTime() {
        return LocalDateTime.now();
    }

    public Date getDateForDelayedStart(TemporalAmount delay) {
        return Date.from(ZonedDateTime.now().toInstant().plus(delay));
    }
}
