package com.sequenceiq.periscope.utils;

import java.text.ParseException;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.quartz.CronExpression;

import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;

public final class DateUtils {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(DateUtils.class);

    private DateUtils() {
        throw new IllegalStateException();
    }

    public static boolean isTrigger(long clusterId, String cron, String timeZone, long monitorUpdateRate) {
        try {
            CronExpression cronExpression = new CronExpression(cron);
            Date currentTime = getCurrentDate(timeZone);
            Date nextTime = cronExpression.getNextValidTimeAfter(currentTime);
            DateTime nextDateTime = getDateTime(nextTime, timeZone).minus(monitorUpdateRate);
            return nextDateTime.toDate().getTime() - currentTime.getTime() < monitorUpdateRate;
        } catch (ParseException e) {
            LOGGER.warn(clusterId, "Invalid cron expression, {}", e.getMessage());
            return false;
        }
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
