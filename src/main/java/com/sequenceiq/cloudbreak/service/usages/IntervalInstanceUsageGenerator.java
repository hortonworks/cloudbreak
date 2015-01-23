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

import com.sequenceiq.cloudbreak.domain.InstanceMetaData;

public class IntervalInstanceUsageGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntervalInstanceUsageGenerator.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final long HOURS_PER_DAY = 24;
    private static final double MS_PER_HOUR = 3600000.0;

    public Map<String, Long> generateUsage(InstanceMetaData instance, Date startTime, Date stopTime) throws ParseException {
        Map<String, Long> dailyInstanceUsages = new HashMap<>();
        Calendar start = getUsageStart(startTime, instance);
        Calendar stop = getUsageStop(stopTime, instance);

        if (start != null && stop != null) {
            doGenerateUsage(instance, dailyInstanceUsages, start, stop);
        }
        return dailyInstanceUsages;
    }

    private Calendar getUsageStart(Date startTime, InstanceMetaData instance) {
        Calendar start = null;
        Long instanceStart = instance.getStartDate();
        Long instanceStop = instance.getTerminationDate();
        long intervalStart = startTime.getTime();
        if (instanceStop == null || intervalStart < instanceStop) {
            if (instanceStart < intervalStart) {
                start = getCalendarInstanceForDate(intervalStart);
            } else {
                start = getCalendarInstanceForDate(instanceStart);
            }
        }
        return start;
    }

    private Calendar getUsageStop(Date stopTime, InstanceMetaData instance) {
        Calendar stop = null;
        Long instanceStop = instance.getTerminationDate();
        long intervalStop = stopTime.getTime();
        if (instanceStop == null) {
            stop = getCalendarInstanceForDate(intervalStop);
        } else if (instanceStop != null && instanceStop < intervalStop) {
            stop = getCalendarInstanceForDate(instanceStop);
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
            //subtract minutes that are overflowed from the start
            stop.add(MINUTE, -start.get(MINUTE));
            // get start day running hours
            runningHours = runningHoursForDay(startAsDate, true);
            dailyInstanceUsages.put(DATE_FORMAT.format(startAsDate), runningHours);
            LOGGER.debug("Instance '{}' ran on the of the start, usage: {}", instance.getId(), runningHours);
            // get stop day running hours
            Date stopAsDate = stop.getTime();
            runningHours = runningHoursForDay(stopAsDate, false);
            if (runningHours > 0) {
                dailyInstanceUsages.put(DATE_FORMAT.format(stopAsDate), runningHours);
                LOGGER.debug("Instance '{}' ran on the of the stop, usage: {}", instance.getId(), runningHours);
            }

            generateAllDayStackUsages(start.getTimeInMillis(), stop.getTimeInMillis(), dailyInstanceUsages, instance);
        }
    }

    private boolean isCalendarsOnTheSameDay(Calendar start, Calendar stop) {
        return start.get(YEAR) == stop.get(YEAR)
                && start.get(MONTH) == start.get(MONTH)
                && start.get(DATE) == stop.get(DATE);
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

    private void generateAllDayStackUsages(long startInMs, long stopInMs, Map<String, Long> dailyInstanceUsages, InstanceMetaData instance) {
        Calendar start = getCalendarInstanceForDate(startInMs);
        start.add(DATE, 1);
        setDayToBeginning(start);
        Calendar end = getCalendarInstanceForDate(stopInMs);
        setDayToBeginning(end);

        for (Date date = start.getTime(); !start.after(end) && !start.equals(end); start.add(DATE, 1), date = start.getTime()) {
            String day = DATE_FORMAT.format(date);
            dailyInstanceUsages.put(day, HOURS_PER_DAY);
            //instance parameter could be removed from this function after test to decrease number of parameters
            LOGGER.debug("Instance '{}' ran on all day, usage: '{}'", instance.getId(), day);
        }
    }

    private void setDayToBeginning(Calendar calendar) {
        calendar.set(HOUR_OF_DAY, 0);
        calendar.set(MINUTE, 0);
        calendar.set(SECOND, 0);
        calendar.set(MILLISECOND, 0);
    }
}
