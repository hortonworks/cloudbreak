package com.sequenceiq.periscope.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Service;

@Service
public class DateTimeUtils {

    public ZonedDateTime getDefaultZonedDateTime() {
        return ZonedDateTime.now();
    }

    public ZonedDateTime getZonedDateTime(Instant instant, String timeZone) {
        return ZonedDateTime.ofInstant(instant, ZoneId.of(timeZone));
    }
}
