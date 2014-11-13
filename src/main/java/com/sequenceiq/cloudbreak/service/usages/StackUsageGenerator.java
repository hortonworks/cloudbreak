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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository;

@Component
public class StackUsageGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackUsageGenerator.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final int HOURS_PER_DAY = 24;
    private static final double HOUR_IN_MS = 3600000.0;

    @Autowired
    private CloudbreakEventRepository eventRepository;

    public List<CloudbreakUsage> generate(List<CloudbreakEvent> stackEvents) {
        List<CloudbreakUsage> dailyCbUsages = new LinkedList<>();
        CloudbreakEvent actEvent = null;
        try {
            CloudbreakEvent start = null;
            for (CloudbreakEvent cbEvent : stackEvents) {
                MDCBuilder.buildMdcContext(cbEvent);
                actEvent = cbEvent;
                if (isStartEvent(cbEvent) && start == null) {
                    start = cbEvent;
                } else if (isStopEvent(cbEvent) && start != null && start.getEventTimestamp().before(cbEvent.getEventTimestamp())) {
                    Map<String, CloudbreakUsage> dailyUsages = getDailyUsagesForBillingPeriod(start.getEventTimestamp(), cbEvent.getEventTimestamp(), cbEvent);
                    dailyCbUsages.addAll(dailyUsages.values());
                    start = null;
                }
            }

            if (start != null) {
                generateRunningStackUsage(dailyCbUsages, start);
            }
        } catch (ParseException e) {
            MDCBuilder.buildMdcContext(actEvent);
            LOGGER.error("Usage generation is failed for stack(id:{})! Invalid date in event(id:{})! Ex: {}", actEvent.getStackId(), actEvent.getId(), e);
            throw new IllegalStateException(e);
        }
        return dailyCbUsages;
    }

    private boolean isStopEvent(CloudbreakEvent event) {
        return event.getEventType().equals(BillingStatus.BILLING_STOPPED.name());
    }

    private boolean isStartEvent(CloudbreakEvent event) {
        return event.getEventType().equals(BillingStatus.BILLING_STARTED.name());
    }

    private void generateRunningStackUsage(List<CloudbreakUsage> dailyCbUsages, CloudbreakEvent startEvent) throws ParseException {
        Calendar cal = Calendar.getInstance();
        setDayToBeginning(cal);
        Date billingStart = startEvent.getEventTimestamp();
        Map<String, CloudbreakUsage> usages = getDailyUsagesForBillingPeriod(billingStart, cal.getTime(), startEvent);
        dailyCbUsages.addAll(usages.values());

        //get overflowed minutes from the start event
        Calendar start = Calendar.getInstance();
        start.setTime(billingStart);
        cal.set(MINUTE, start.get(MINUTE));
        //save billing start event for daily usage generation
        CloudbreakEvent newBilling = createBillingStarterCloudbreakEvent(startEvent, cal);
        eventRepository.save(newBilling);
        LOGGER.debug("BILLING_STARTED is created with date:{} for running stack {}.", cal.getTime(), newBilling.getStackId());
    }

    private Map<String, CloudbreakUsage> getDailyUsagesForBillingPeriod(Date startTime, Date stopTime, CloudbreakEvent event) throws ParseException {
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
            LOGGER.debug("Stack ran less than a day. runningHours: {}", runningHours);
            CloudbreakUsage cbUsageStart = getCloudbreakUsage(event, runningHours, startTime);
            dailyStackUsageMap.put(DATE_FORMAT.format(startTime), cbUsageStart);
        } else {
            // get start day running hours
            runningHours = runningHoursForDay(startTime, true);
            CloudbreakUsage startDayUsage = getCloudbreakUsage(event, runningHours, startTime);
            dailyStackUsageMap.put(DATE_FORMAT.format(startTime), startDayUsage);
            LOGGER.debug("Generated start day usage: {}", startDayUsage);
            // get stop day running hours
            stopTime = stop.getTime();
            runningHours = runningHoursForDay(stopTime, false);
            if (runningHours > 0) {
                Date startOfStopTimeDay = DATE_FORMAT.parse(DATE_FORMAT.format(stopTime));
                CloudbreakUsage stopDayUsage = getCloudbreakUsage(event, runningHours, startOfStopTimeDay);
                dailyStackUsageMap.put(DATE_FORMAT.format(stopTime), stopDayUsage);
                LOGGER.debug("Generated stop day usage: {}", stopDayUsage);
            }

            generateAllDayStackUsages(startTime, stopTime, event, dailyStackUsageMap);
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

    private void setDayToBeginning(Calendar calendar) {
        calendar.set(HOUR_OF_DAY, 0);
        calendar.set(MINUTE, 0);
        calendar.set(SECOND, 0);
        calendar.set(MILLISECOND, 0);
    }

    private CloudbreakUsage getCloudbreakUsage(CloudbreakEvent event, long runningHours, Date day) {
        CloudbreakUsage usage = new CloudbreakUsage();
        usage.setOwner(event.getOwner());
        usage.setAccount(event.getAccount());
        usage.setBlueprintId(event.getBlueprintId());
        usage.setBlueprintName(event.getBlueprintName());
        usage.setCloud(event.getCloud());
        usage.setZone(event.getRegion());
        usage.setMachineType(event.getVmType());
        usage.setRunningHours(String.valueOf(runningHours));
        usage.setDay(day);
        usage.setStackId(event.getStackId());
        usage.setStackStatus(event.getStackStatus());
        usage.setStackName(event.getStackName());
        return usage;
    }

    private CloudbreakEvent createBillingStarterCloudbreakEvent(CloudbreakEvent startEvent, Calendar cal) {
        CloudbreakEvent event = new CloudbreakEvent();
        event.setEventType(BillingStatus.BILLING_STARTED.name());
        event.setAccount(startEvent.getAccount());
        event.setOwner(startEvent.getOwner());
        event.setEventMessage(startEvent.getEventMessage());
        event.setBlueprintId(startEvent.getBlueprintId());
        event.setBlueprintName(startEvent.getBlueprintName());
        event.setEventTimestamp(cal.getTime());
        event.setVmType(startEvent.getVmType());
        event.setCloud(startEvent.getCloud());
        event.setRegion(startEvent.getRegion());
        event.setStackId(startEvent.getStackId());
        event.setStackStatus(startEvent.getStackStatus());
        event.setStackName(startEvent.getStackName());
        return event;
    }
}
