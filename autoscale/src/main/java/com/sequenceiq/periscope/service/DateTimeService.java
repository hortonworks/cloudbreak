package com.sequenceiq.periscope.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

@Service
public class DateTimeService {

    public ZonedDateTime getDefaultZonedDateTime() {
        return ZonedDateTime.now();
    }

    public ZonedDateTime getZonedDateTime(Instant instant, String timeZone) {
        return ZonedDateTime.ofInstant(instant, ZoneId.of(timeZone));
    }

    public ZonedDateTime getNextSecound(ZonedDateTime now) {
        return now.withNano(0).plus(1L, ChronoUnit.SECONDS);
    }
}
