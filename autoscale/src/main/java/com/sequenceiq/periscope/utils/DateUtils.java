package com.sequenceiq.periscope.utils;

import java.text.ParseException;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DateUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateUtils.class);

    private DateUtils() {
        throw new IllegalStateException();
    }

    public static boolean isTrigger(String cron, String timeZone, long monitorUpdateRate) {
        try {
            CronExpression cronExpression = getCronExpression(cron);
            Date currentTime = getCurrentDate(timeZone);
            Date nextTime = cronExpression.getNextValidTimeAfter(currentTime);
            DateTime nextDateTime = getDateTime(nextTime, timeZone).minus(monitorUpdateRate);
            long interval = nextDateTime.toDate().getTime() - currentTime.getTime();
            return interval > 0 && interval < monitorUpdateRate;
        } catch (ParseException e) {
            LOGGER.warn("Invalid cron expression, {}", e.getMessage());
            return false;
        }
    }

    public static CronExpression getCronExpression(String cron) throws ParseException {
        return new CronExpression(cron);
    }

    private static DateTime getDateTime(Date date, String timeZone) {
        return new DateTime(date).withZone(getTimeZone(timeZone));
    }

    private static Date getCurrentDate(String timeZone) {
        return getCurrentDateTime(timeZone).toLocalDateTime().toDate();
    }

    private static DateTime getCurrentDateTime(String timeZone) {
        return DateTime.now(getTimeZone(timeZone));
    }

    private static DateTimeZone getTimeZone(String timeZone) {
        return DateTimeZone.forID(timeZone);
    }

}
