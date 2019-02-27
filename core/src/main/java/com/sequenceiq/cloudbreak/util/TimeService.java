package com.sequenceiq.cloudbreak.util;

import java.time.Instant;
import java.time.temporal.TemporalAmount;

import org.springframework.stereotype.Service;

@Service
public class TimeService {

    public Long getTimeInMillis() {
        return Instant.now().toEpochMilli();
    }

    public Long nowMinus(TemporalAmount amount) {
        return Instant.now().minus(amount).toEpochMilli();
    }
}
