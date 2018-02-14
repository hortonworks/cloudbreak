package com.sequenceiq.periscope.utils;

import java.text.ParseException;
import java.util.Date;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Service;

@Service
public final class DateUtils {

    public static final int DAY_OF_WEEK_FIELD = 5;

    public static final int MINIMAL_CRON_SEGMENT_LENGTH = 6;

    public static final int MINIMAL_USER_DEFINED_CRON_SEGMENT_LENGTH = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(DateUtils.class);

    @Inject
    private DateTimeUtils dateTimeUtils;

    public boolean isTrigger(String cron, String timeZone, long monitorUpdateRate) {
        try {
            CronSequenceGenerator cronExpression = getCronExpression(cron);
            DateTime currentTime = dateTimeUtils.getCurrentDate(timeZone);
            Date nextTime = cronExpression.next(currentTime.toDate());
            DateTime nextDateTime = dateTimeUtils.getDateTime(nextTime, timeZone).minus(monitorUpdateRate);
            long interval = nextDateTime.toDate().getTime() - currentTime.toDate().getTime();
            return interval > 0 && interval < monitorUpdateRate;
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
