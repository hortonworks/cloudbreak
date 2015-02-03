package com.sequenceiq.cloudbreak.service.usages;


import static java.util.Calendar.DATE;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Component
public class IntervalInstanceUsageGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntervalInstanceUsageGenerator.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final long HOURS_PER_DAY = 24;
    private static final double MS_PER_HOUR = 3600000.0;

    public Map<String, Long> getInstanceHours(InstanceMetaData instance, Date startTime, Date stopTime) throws ParseException {
        MDCBuilder.buildMdcContext(instance);
        Map<String, Long> dailyInstanceUsages = new HashMap<>();
        Calendar start = getUsageStart(startTime, stopTime, instance);
        Calendar stop = getUsageStop(startTime, stopTime, instance);

        if (start != null && stop != null) {
            doGenerateUsage(instance, dailyInstanceUsages, start, stop);
        }
        return dailyInstanceUsages;
    }

    private Calendar getUsageStart(Date startTime, Date stopTime, InstanceMetaData instance) {
        Calendar start = null;
        Long instanceStart = instance.getStartDate();
        Long intervalStart = startTime.getTime();
        Long intervalStop = stopTime.getTime();
        if (instanceStart != null && intervalStop > instanceStart) {
            if (instanceStart < intervalStart) {
                start = getCalendarInstanceForDate(intervalStart);
            } else {
                start = getCalendarInstanceForDate(instanceStart);
            }
        }
        return start;
    }

    private Calendar getUsageStop(Date startTime, Date stopTime, InstanceMetaData instance) {
        Calendar stop = null;
        Long instanceStop = instance.getTerminationDate();
        Long intervalStart = startTime.getTime();
        Long intervalStop = stopTime.getTime();
        if (instanceStop == null || intervalStart < instanceStop) {
            if (instanceStop != null && instanceStop < intervalStop) {
                stop = getCalendarInstanceForDate(instanceStop);
            } else {
                stop = getCalendarInstanceForDate(intervalStop);
            }
        }
        return stop;
    }

    private Calendar getCalendarInstanceForDate(long timeInMs) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeInMs);
        return cal;
    }

    private void doGenerateUsage(InstanceMetaData instance, Map<String, Long> dailyInstanceUsages, Calendar start, Calendar stop) throws ParseException {
        long runningHours = 0;
        Date startAsDate = start.getTime();
        if (isCalendarsOnTheSameDay(start, stop)) {
            runningHours = millisToCeiledHours(stop.getTimeInMillis() - start.getTimeInMillis());
            LOGGER.debug("Instance '{}' ran less than a day, usage: {}", instance.getId(), runningHours);
            dailyInstanceUsages.put(DATE_FORMAT.format(startAsDate), runningHours);
        } else {
            // get start day running hours
            runningHours = runningHoursForDay(startAsDate, true);
            dailyInstanceUsages.put(DATE_FORMAT.format(startAsDate), runningHours);
            LOGGER.debug("Instance '{}' ran on the start day, usage: {}", instance.getId(), runningHours);
            // get stop day running hours
            subtractStartOverFlowedTimeFromStop(start, stop);
            if (!isCalendarsOnTheSameDay(start, stop)) {
                Date stopAsDate = stop.getTime();
                runningHours = runningHoursForDay(stopAsDate, false);
                dailyInstanceUsages.put(DATE_FORMAT.format(stopAsDate), runningHours);
                LOGGER.debug("Instance '{}' ran on the day of the termination, usage: {}", instance.getId(), runningHours);
            }

            generateAllDayStackUsages(start.getTimeInMillis(), stop.getTimeInMillis(), dailyInstanceUsages);
        }
    }

    private boolean isCalendarsOnTheSameDay(Calendar start, Calendar stop) {
        return start.get(YEAR) == stop.get(YEAR)
                && start.get(MONTH) == stop.get(MONTH)
                && start.get(DATE) == stop.get(DATE);
    }

    private void subtractStartOverFlowedTimeFromStop(Calendar start, Calendar stop) {
        stop.add(MINUTE, -start.get(MINUTE));
        stop.add(SECOND, -start.get(SECOND));
        stop.add(MILLISECOND, -start.get(MILLISECOND));
    }

    private long runningHoursForDay(Date date, boolean startDay) throws ParseException {
        String dayAsStr = DATE_FORMAT.format(date);
        long dayStartOrEndInMillis = DATE_FORMAT.parse(dayAsStr).getTime();
        if (startDay) {
            dayStartOrEndInMillis += TimeUnit.HOURS.toMillis(HOURS_PER_DAY);
        }
        long hourInMillis = date.getTime() - dayStartOrEndInMillis;
        return millisToCeiledHours(hourInMillis);
    }

    private long millisToCeiledHours(long hourInMillis) {
        long absHourInMillis = Math.abs(hourInMillis);
        Double ceiledHours = Math.ceil(absHourInMillis / MS_PER_HOUR);
        return ceiledHours.longValue();
    }

    private void generateAllDayStackUsages(long startInMs, long stopInMs, Map<String, Long> dailyInstanceUsages) {
        Calendar start = getCalendarInstanceForDate(startInMs);
        start.add(DATE, 1);
        setDayToBeginning(start);
        Calendar end = getCalendarInstanceForDate(stopInMs);
        setDayToBeginning(end);

        for (Date date = start.getTime(); !start.after(end) && !start.equals(end); start.add(DATE, 1), date = start.getTime()) {
            String day = DATE_FORMAT.format(date);
            dailyInstanceUsages.put(day, HOURS_PER_DAY);
            LOGGER.debug("Instance ran on all day, usage: '{}'", day);
        }
    }

    private void setDayToBeginning(Calendar calendar) {
        calendar.set(HOUR_OF_DAY, 0);
        calendar.set(MINUTE, 0);
        calendar.set(SECOND, 0);
        calendar.set(MILLISECOND, 0);
    }
}
