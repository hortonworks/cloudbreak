package com.sequenceiq.periscope.service;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

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

    @Inject
    private DateTimeService dateTimeService;

    public boolean isTrigger(TimeAlert alert, long monitorUpdateRate) {
        return isTrigger(alert, monitorUpdateRate, dateTimeService.getDefaultZonedDateTime());
    }

    public boolean isTrigger(TimeAlert alert, long monitorUpdateRate, ZonedDateTime currentTime) {
        try {
            String timeZone = alert.getTimeZone();
            CronSequenceGenerator cronExpression = getCronExpression(alert.getCron());
            ZonedDateTime zonedCurrentTime = dateTimeService.getZonedDateTime(currentTime.toInstant(), timeZone);
            LocalDateTime startTimeOfTheMonitorInterval = zonedCurrentTime.toLocalDateTime().minus(monitorUpdateRate, ChronoUnit.MILLIS);
            Date startDate = Date.from(startTimeOfTheMonitorInterval.toInstant(currentTime.getOffset()));
            Date nextTime = cronExpression.next(startDate);
            ZonedDateTime zonedNextTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(nextTime.getTime()), currentTime.getZone()).atZone(ZoneId.of(timeZone));
            long interval = (zonedCurrentTime.toEpochSecond() - zonedNextTime.toEpochSecond()) * TimeUtil.SECOND_TO_MILLISEC;
            LOGGER.debug("Time alert '{}' next firing at '{}' compared to current time '{}' in timezone '{}'",
                    alert.getName(), zonedNextTime, zonedCurrentTime, timeZone);
            return interval >= 0L && interval < monitorUpdateRate;
        } catch (ParseException e) {
            LOGGER.info("Invalid cron expression, {}", e.getMessage());
            return false;
        }
    }

    public CronSequenceGenerator getCronExpression(String cron) throws ParseException {
        String[] splits = cron.split("\\s+");
        if (splits.length < MINIMAL_CRON_SEGMENT_LENGTH && splits.length > MINIMAL_USER_DEFINED_CRON_SEGMENT_LENGTH) {
            for (int i = splits.length; i < MINIMAL_CRON_SEGMENT_LENGTH; i++) {
                cron = i == DAY_OF_WEEK_FIELD ? String.format("%s ?", cron) : String.format("%s *", cron);
            }
        }
        try {
            return new CronSequenceGenerator(cron);
        } catch (Exception ex) {
            throw new ParseException(ex.getMessage(), 0);
        }
    }

    public void validateTimeZone(String timeZone) throws ParseException {
        try {
            if (timeZone != null) {
                ZoneId.of(timeZone);
            }
        } catch (Exception ex) {
            throw new ParseException(ex.getMessage(), 0);
        }
    }
}
