package com.sequenceiq.periscope.utils;

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

@Service
public final class DateUtils {

    public static final int DAY_OF_WEEK_FIELD = 5;

    public static final int MINIMAL_CRON_SEGMENT_LENGTH = 6;

    public static final int MINIMAL_USER_DEFINED_CRON_SEGMENT_LENGTH = 3;

    public static final int SECOND_TO_MILLISEC = 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(DateUtils.class);

    @Inject
    private DateTimeUtils dateTimeUtils;

    public boolean isTrigger(TimeAlert alert, long monitorUpdateRate) {
        try {
            String timeZone = alert.getTimeZone();
            CronSequenceGenerator cronExpression = getCronExpression(alert.getCron());
            ZonedDateTime currentTime = dateTimeUtils.getDefaultZonedDateTime();
            ZonedDateTime zonedCurrentTime = dateTimeUtils.getZonedDateTime(currentTime.toInstant(), timeZone);
            LocalDateTime startTimeOfTheMonitorInterval = zonedCurrentTime.toLocalDateTime().minus(monitorUpdateRate, ChronoUnit.MILLIS);
            Date startDate = Date.from(startTimeOfTheMonitorInterval.toInstant(currentTime.getOffset()));
            Date nextTime = cronExpression.next(startDate);
            ZonedDateTime zonedNextTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(nextTime.getTime()), currentTime.getZone()).atZone(ZoneId.of(timeZone));
            long interval = (zonedCurrentTime.toEpochSecond() - zonedNextTime.toEpochSecond()) * SECOND_TO_MILLISEC;
            LOGGER.info("Time alert '{}' next firing at '{}' compared to current time '{}' in timezone '{}'",
                    alert.getName(), zonedNextTime, zonedCurrentTime, timeZone);
            return interval >= 0 && interval < monitorUpdateRate;
        } catch (ParseException e) {
            LOGGER.warn("Invalid cron expression, {}", e.getMessage());
            return false;
        }
    }

    public CronSequenceGenerator getCronExpression(String cron) throws ParseException {

        String[] splits = cron.split("\\s+");
        if (splits.length < MINIMAL_CRON_SEGMENT_LENGTH && splits.length > MINIMAL_USER_DEFINED_CRON_SEGMENT_LENGTH) {
            for (int i = splits.length; i < MINIMAL_CRON_SEGMENT_LENGTH; i++) {
                switch (i) {
                    case DAY_OF_WEEK_FIELD:  cron = String.format("%s ?", cron);
                        break;
                    default: cron = String.format("%s *", cron);
                        break;
                }
            }
        }
        try {
            return new CronSequenceGenerator(cron);
        } catch (Exception ex) {
            throw new ParseException(ex.getMessage(), 0);
        }

    }



}
