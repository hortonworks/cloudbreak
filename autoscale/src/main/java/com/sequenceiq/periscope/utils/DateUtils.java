package com.sequenceiq.periscope.utils;

import java.text.ParseException;
import java.util.Date;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public final class DateUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateUtils.class);

    @Inject
    private DateTimeUtils dateTimeUtils;

    public boolean isTrigger(String cron, String timeZone, long monitorUpdateRate) {
        try {
            CronExpression cronExpression = getCronExpression(cron);
            DateTime currentTime = dateTimeUtils.getCurrentDate(timeZone);
            Date nextTime = cronExpression.getNextValidTimeAfter(currentTime.toDate());
            DateTime nextDateTime = dateTimeUtils.getDateTime(nextTime, timeZone).minus(monitorUpdateRate);
            long interval = nextDateTime.toDate().getTime() - currentTime.toDate().getTime();
            return interval > 0 && interval < monitorUpdateRate;
        } catch (ParseException e) {
            LOGGER.warn("Invalid cron expression, {}", e.getMessage());
            return false;
        }
    }

    public CronExpression getCronExpression(String cron) throws ParseException {
        return new CronExpression(cron);
    }



}
