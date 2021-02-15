package com.sequenceiq.periscope.service;

import static java.lang.String.format;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.utils.TimeUtil;

@Service
public final class DateService {

    public static final int DAY_OF_WEEK_FIELD = 5;

    public static final int MINIMAL_CRON_SEGMENT_LENGTH = 6;

    public static final int MINIMAL_USER_DEFINED_CRON_SEGMENT_LENGTH = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(DateService.class);

    //BE AWARE OF THAT this is a state holder and this fix(CB-11143) won't work in Periscope HA environments.
    private final Map<Long, ZonedDateTime> zonedLastEvaluatedByAlertId = new ConcurrentHashMap<>();

    @Inject
    private DateTimeService dateTimeService;

    public boolean isTrigger(TimeAlert alert, long monitorUpdateRate) {
        return isTrigger(alert, monitorUpdateRate, dateTimeService.getDefaultZonedDateTime());
    }

    public boolean isTrigger(TimeAlert alert, long defaultMonitorUpdateRate, ZonedDateTime currentTime) {
        try {
            String timeZone = alert.getTimeZone();
            CronSequenceGenerator cronExpression = getCronExpression(alert.getCron());
            ZonedDateTime zonedCurrentTime = dateTimeService.getZonedDateTime(currentTime.toInstant(), timeZone);
            long calculatedMonitorUpdateRate = calculateMonitorUpdateRate(alert.getId(), zonedCurrentTime, defaultMonitorUpdateRate);
            LocalDateTime startTimeOfTheMonitorInterval = zonedCurrentTime.toLocalDateTime().minus(calculatedMonitorUpdateRate, ChronoUnit.MILLIS);
            Date startDate = Date.from(startTimeOfTheMonitorInterval.toInstant(currentTime.getOffset()));
            Date nextTime = cronExpression.next(startDate);
            ZonedDateTime zonedNextTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(nextTime.getTime()), currentTime.getZone()).atZone(ZoneId.of(timeZone));
            long interval = (zonedCurrentTime.toEpochSecond() - zonedNextTime.toEpochSecond()) * TimeUtil.SECOND_TO_MILLISEC;
            zonedLastEvaluatedByAlertId.put(alert.getId(), zonedCurrentTime);
            LOGGER.info("Time alert '{}' next firing at '{}' compared to current time '{}' in timezone '{}', considered with monitor update rate seconds'{}' "
                            + "and interval start time '{}'",
                    alert.getName(), zonedNextTime, zonedCurrentTime, timeZone, calculatedMonitorUpdateRate, startTimeOfTheMonitorInterval);
            return interval >= 0L && interval < calculatedMonitorUpdateRate;
        } catch (ParseException e) {
            LOGGER.warn("Invalid cron expression, {}", e.getMessage());
            return false;
        }
    }

    public CronSequenceGenerator getCronExpression(String cron) throws ParseException {
        String[] splits = cron.split("\\s+");
        if (splits.length < MINIMAL_CRON_SEGMENT_LENGTH && splits.length > MINIMAL_USER_DEFINED_CRON_SEGMENT_LENGTH) {
            for (int i = splits.length; i < MINIMAL_CRON_SEGMENT_LENGTH; i++) {
                cron = i == DAY_OF_WEEK_FIELD ? format("%s ?", cron) : format("%s *", cron);
            }
        }
        try {
            return new CronSequenceGenerator(cron);
        } catch (Exception ex) {
            throw new ParseException(ex.getMessage(), 0);
        }
    }

    public void validateCronExpression(String cron) throws ParseException {
        String[] splits = cron.split("\\s+");
        if (splits.length != MINIMAL_CRON_SEGMENT_LENGTH) {
            throw new ParseException(format("Invalid length of cron expression, expected %s but found %s", MINIMAL_CRON_SEGMENT_LENGTH, splits.length), 0);
        }
    }

    public void cleanupAlert(Long alertId) {
        zonedLastEvaluatedByAlertId.remove(alertId);
    }

    private long calculateMonitorUpdateRate(Long alertId, ZonedDateTime zonedCurrentTime, long defaultMonitorUpdateRate) {
        long calculatedMonitorUpdateRate = defaultMonitorUpdateRate;
        ZonedDateTime previouslyEvaluated = zonedLastEvaluatedByAlertId.get(alertId);
        if (previouslyEvaluated != null) {
            long interval = (zonedCurrentTime.toEpochSecond() - previouslyEvaluated.toEpochSecond()) * TimeUtil.SECOND_TO_MILLISEC;
            if (interval > defaultMonitorUpdateRate) {
                LOGGER.debug("The monitor update rate needs to be increased to cover time between now and the previous cron evaluation ('{}')", interval);
                calculatedMonitorUpdateRate = interval;
            }
        }
        return calculatedMonitorUpdateRate;
    }
}
