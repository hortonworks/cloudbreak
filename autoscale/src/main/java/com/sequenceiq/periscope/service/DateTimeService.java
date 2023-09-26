package com.sequenceiq.periscope.service;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

@Service
public class DateTimeService {

    public ZonedDateTime getDefaultZonedDateTime() {
        return ZonedDateTime.now();
    }

    public ZonedDateTime getNextSecound(ZonedDateTime now) {
        return now.withNano(0).plus(1L, ChronoUnit.SECONDS);
    }
}
