package com.sequenceiq.cloudbreak.service.usages;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;

@Service
public class UsageTimeService {

    public static final int MINUTES_IN_HOUR = 60;

    public long daysBetweenDateAndNow(Date date) {
        ZonedDateTime dt = date.toInstant().atZone(ZoneId.systemDefault()).with(this::startOfDay);
        ZonedDateTime today = LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault());
        Duration d = Duration.between(dt, today);
        return d.toDays();
    }

    public long convertToInstanceHours(Duration d) {
        return (long) Math.ceil(d.toMinutes() * 1.0 / MINUTES_IN_HOUR);
    }

    public Duration calculateNewDuration(CloudbreakUsage usage) {
        Duration d = Duration.between(usage.getPeriodStarted().toInstant(), getEndInstant(usage.getPeriodStarted()));
        Duration already = stringToDuration(usage.getDuration());
        return already.plus(d.multipliedBy(usage.getInstanceNum()));
    }

    public Instant getStartOfDay() {
        return ZonedDateTime.now().with(this::startOfDay).toInstant();
    }

    private Temporal startOfDay(Temporal input) {
        LocalDateTime t = LocalDate.from(input).atStartOfDay();
        return input.with(t);
    }

    private Temporal endOfDay(Temporal input) {
        LocalDateTime t = LocalDate.from(input)
                .plusDays(1)
                .atStartOfDay()
                .minusSeconds(1);
        return input.with(t);
    }

    private Duration stringToDuration(String duration) {
        return Optional.ofNullable(duration).map(Duration::parse).orElse(Duration.ZERO);
    }

    private Instant getEndInstant(Date start) {
        ZonedDateTime startDt = start.toInstant().atZone(ZoneId.systemDefault());
        ZonedDateTime nowDt = ZonedDateTime.now();
        if (sameDay(startDt, nowDt)) {
            return nowDt.toInstant();
        }
        return startDt.with(this::endOfDay).toInstant();
    }

    private boolean sameDay(ZonedDateTime t1, ZonedDateTime t2) {
        return t1.getYear() == t2.getYear() && t1.getDayOfYear() == t2.getDayOfYear();
    }
}
