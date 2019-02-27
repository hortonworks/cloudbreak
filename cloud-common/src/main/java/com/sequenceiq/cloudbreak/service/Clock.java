package com.sequenceiq.cloudbreak.service;

import java.time.Instant;
import java.time.temporal.TemporalAmount;

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
}
