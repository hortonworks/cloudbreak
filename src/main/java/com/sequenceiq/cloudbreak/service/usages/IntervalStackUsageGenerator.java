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

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;

@Component
public class IntervalStackUsageGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntervalStackUsageGenerator.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final int HOURS_PER_DAY = 24;
    private static final double HOUR_IN_MS = 3600000.0;

    public Map<String, CloudbreakUsage> getUsages(Date startTime, Date stopTime, CloudbreakEvent startEvent) throws ParseException {
        Map<String, CloudbreakUsage> dailyStackUsageMap = new HashMap<>();
        Calendar start = Calendar.getInstance();
        start.setTime(startTime);
        Calendar stop = Calendar.getInstance();
        stop.setTime(stopTime);
        //subtract minutes that are overflowed from the start
        stop.add(MINUTE, -start.get(MINUTE));
        LOGGER.debug("Generate daily usage for startTime: {}, stopTime: {}", startTime, stopTime);

        long runningHours = 0;

        if (isCalendarsOnTheSameDay(start, stop)) {
            runningHours = millisToCeiledHours(stopTime.getTime() - startTime.getTime());
            CloudbreakUsage usage = getCloudbreakUsage(startEvent, runningHours, startTime);
            LOGGER.debug("Stack ran less than a day, usage: {}", usage);
            dailyStackUsageMap.put(DATE_FORMAT.format(startTime), usage);
        } else {
            // get start day running hours
            runningHours = runningHoursForDay(startTime, true);
            CloudbreakUsage startDayUsage = getCloudbreakUsage(startEvent, runningHours, startTime);
            dailyStackUsageMap.put(DATE_FORMAT.format(startTime), startDayUsage);
            LOGGER.debug("Generated start day usage: {}", startDayUsage);
            // get stop day running hours
            stopTime = stop.getTime();
            runningHours = runningHoursForDay(stopTime, false);
            if (runningHours > 0) {
                Date startOfStopTimeDay = DATE_FORMAT.parse(DATE_FORMAT.format(stopTime));
                CloudbreakUsage stopDayUsage = getCloudbreakUsage(startEvent, runningHours, startOfStopTimeDay);
                dailyStackUsageMap.put(DATE_FORMAT.format(stopTime), stopDayUsage);
                LOGGER.debug("Generated stop day usage: {}", stopDayUsage);
            }

            generateAllDayStackUsages(startTime, stopTime, startEvent, dailyStackUsageMap);
        }
        return dailyStackUsageMap;
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
        Double ceiledHours = Math.ceil(absHourInMillis / HOUR_IN_MS);
        return ceiledHours.longValue();
    }

    private void generateAllDayStackUsages(Date startTime, Date stopTime, CloudbreakEvent prototype, Map<String, CloudbreakUsage> dailyStackUsageMap) {
        Calendar start = Calendar.getInstance();
        start.setTime(startTime);
        start.add(DATE, 1);
        setDayToBeginning(start);
        Calendar end = Calendar.getInstance();
        end.setTime(stopTime);
        setDayToBeginning(end);

        for (Date date = start.getTime(); !start.after(end) && !start.equals(end); start.add(DATE, 1), date = start.getTime()) {
            CloudbreakUsage usage = getCloudbreakUsage(prototype, HOURS_PER_DAY, date);
            dailyStackUsageMap.put(DATE_FORMAT.format(date), usage);
            LOGGER.debug("Generated daily usage: {}", usage);
        }
    }

    private CloudbreakUsage getCloudbreakUsage(CloudbreakEvent event, long runningHours, Date day) {
        long nodesRunningHours = runningHours * event.getNodeCount();
        CloudbreakUsage usage = new CloudbreakUsage();
        usage.setOwner(event.getOwner());
        usage.setAccount(event.getAccount());
        usage.setProvider(event.getCloud());
        usage.setRegion(event.getRegion());
        usage.setInstanceHours(nodesRunningHours);
        usage.setDay(day);
        usage.setStackId(event.getStackId());
        usage.setStackName(event.getStackName());
        return usage;
    }

    private void setDayToBeginning(Calendar calendar) {
        calendar.set(HOUR_OF_DAY, 0);
        calendar.set(MINUTE, 0);
        calendar.set(SECOND, 0);
        calendar.set(MILLISECOND, 0);
    }
}
