package com.sequenceiq.cloudbreak.service.usages;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository;

@Service
public class DefaultCloudbreakUsageGeneratorService implements CloudbreakUsageGeneratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudbreakUsagesRetrievalService.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final int HOURS_IN_DAY = 24;

    @Autowired
    private CloudbreakUsageRepository usageRepository;

    @Autowired
    private CloudbreakEventRepository eventRepository;

    @Override
    public List<CloudbreakUsage> generateCloudbreakUsages(String user) {
        LOGGER.info("Generating user usage for: {}", user);
        List<CloudbreakUsage> usageList = new ArrayList<>();
        // retrieve all events for the user
        List<CloudbreakEvent> cloudbreakEvents = eventRepository.cloudbreakEvents(user);
        // split events by stacks
        Map<Long, List<CloudbreakEvent>> stackEvents = splitCloudbreakEventsByStack(cloudbreakEvents);

        // iterate over events by stacks and generate usages
        for (Map.Entry<Long, List<CloudbreakEvent>> stackEventEntry : stackEvents.entrySet()) {
            LOGGER.debug("Processing stackId {} for userid {}", stackEventEntry.getKey(), user);
            Map<String, CloudbreakUsage> stackDailyUsages = processStackEvents(stackEventEntry.getValue());
            usageList.addAll(stackDailyUsages.values());
        }
        LOGGER.debug("usages: {}", usageList);
        usageRepository.save(usageList);
        return usageList;
    }

    private CloudbreakUsage usageFromEvent(CloudbreakEvent cbEvent) {
        CloudbreakUsage usage = new CloudbreakUsage();

        usage.setOwner(cbEvent.getOwner());
        usage.setAccount(cbEvent.getAccount());

        usage.setBlueprintId(cbEvent.getBlueprintId());
        usage.setBlueprintName(cbEvent.getBlueprintName());

        usage.setCloud(cbEvent.getCloud());
        usage.setZone(cbEvent.getRegion());
        usage.setMachineType(cbEvent.getVmType());
        return usage;
    }


    private Map<Long, List<CloudbreakEvent>> splitCloudbreakEventsByStack(List<CloudbreakEvent> userStackEvents) {
        Map<Long, List<CloudbreakEvent>> stackIdToCbEventMap = new HashMap<>();
        for (CloudbreakEvent cbEvent : userStackEvents) {
            LOGGER.debug("Processing stack {} for user {}", cbEvent.getStackId(), cbEvent.getOwner());
            if (!stackIdToCbEventMap.containsKey(cbEvent.getStackId())) {
                LOGGER.debug("New stack {} for user {}", cbEvent.getStackId(), cbEvent.getOwner());
                stackIdToCbEventMap.put(cbEvent.getStackId(), new ArrayList<CloudbreakEvent>());
            }
            stackIdToCbEventMap.get(cbEvent.getStackId()).add(cbEvent);
        }
        LOGGER.debug("The user has {} stacks.", stackIdToCbEventMap.keySet().size());
        return stackIdToCbEventMap;
    }

    private Map<String, CloudbreakUsage> processStackEvents(List<CloudbreakEvent> stackEvents) {
        Map<String, CloudbreakUsage> dailyCbUsagesMap = new HashMap<>();
        Date startTime = getStackStartTime(stackEvents);
        Date stopTime = getStackStopTime(stackEvents);
        if (startTime.after(stopTime)) {
            throw new IllegalStateException("Stack start time after stop time!");
        }
        try {
            dailyCbUsagesMap = generateDailyUsagesForStack(startTime, stopTime, stackEvents.iterator().next());
        } catch (ParseException e) {
            LOGGER.error("Invalid date in event! Ex: {}", e);
            throw new IllegalStateException(e);
        }
        return dailyCbUsagesMap;
    }

    private Map<String, CloudbreakUsage> generateDailyUsagesForStack(Date startTime, Date stopTime, CloudbreakEvent prototype) throws ParseException {
        Map<String, CloudbreakUsage> dailyStackUsageMap = new HashMap<>();
        long stackRunningTimeMs = stopTime.getTime() - startTime.getTime();
        long runningHours = 0;
        // getting stack-days
        long days = TimeUnit.MILLISECONDS.toDays(stackRunningTimeMs);

        if (days < 1) {
            runningHours = TimeUnit.MILLISECONDS.toHours(stopTime.getTime() - startTime.getTime());
            LOGGER.debug("Stack ran less than a day. runningHours: {}", runningHours);
            CloudbreakUsage cbUsageStart = getCloudbreakUsage(prototype, runningHours, startTime);
            dailyStackUsageMap.put(DATE_FORMAT.format(startTime), cbUsageStart);
        } else {
            LOGGER.debug("Stack run spans multiple days. startTime: {}, stopTime: {}", startTime, stopTime);
            // get start day running hours
            runningHours = runningHoursForStartOrStopDay(startTime, true);
            CloudbreakUsage startDayUsage = getCloudbreakUsage(prototype, runningHours, startTime);
            dailyStackUsageMap.put(DATE_FORMAT.format(startTime), startDayUsage);
            LOGGER.debug("Generated start day usage: {}", startDayUsage);
            // get stop day running hours
            runningHours = runningHoursForStartOrStopDay(stopTime, false);
            CloudbreakUsage stopDayUsage = getCloudbreakUsage(prototype, runningHours, stopTime);
            dailyStackUsageMap.put(DATE_FORMAT.format(stopTime), stopDayUsage);
            LOGGER.debug("Generated stop day usage: {}", stopDayUsage);

            generateAllDayStackUsages(startTime, stopTime, prototype, dailyStackUsageMap);
        }
        return dailyStackUsageMap;
    }

    private void generateAllDayStackUsages(Date startTime, Date stopTime, CloudbreakEvent prototype, Map<String, CloudbreakUsage> dailyStackUsageMap) {
        // generate all day running hours
        Calendar start = Calendar.getInstance();
        start.setTime(startTime);
        start.add(Calendar.DATE, 1);
        Calendar end = Calendar.getInstance();
        end.setTime(stopTime);
        end.add(Calendar.DATE, -1);

        for (Date date = start.getTime(); !start.after(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
            CloudbreakUsage usage = getCloudbreakUsage(prototype, HOURS_IN_DAY, date);
            dailyStackUsageMap.put(DATE_FORMAT.format(date), usage);
            LOGGER.debug("Generated daily usage: {}", usage);
        }
    }

    private long runningHoursForStartOrStopDay(Date date, boolean start) throws ParseException {
        String dayAsStr = DATE_FORMAT.format(date);
        long dayStartOrEndInMillis = DATE_FORMAT.parse(dayAsStr).getTime();
        if (start) {
            // end of the day
            dayStartOrEndInMillis += TimeUnit.HOURS.toMillis(HOURS_IN_DAY) - 1;
        }
        return TimeUnit.MILLISECONDS.toHours(Math.abs(date.getTime() - dayStartOrEndInMillis));
    }

    private CloudbreakUsage getCloudbreakUsage(CloudbreakEvent prototype, long runningHours, Date day) {
        CloudbreakUsage dailyUsage = usageFromEvent(prototype);
        dailyUsage.setRunningHours(String.valueOf(runningHours));
        dailyUsage.setDay(day);
        return dailyUsage;
    }

    private Date getStackStopTime(List<CloudbreakEvent> stackEvents) {
        Date stopTime = null;
        for (CloudbreakEvent event : stackEvents) {
            if (isStopEvent(event)) {
                if (stopTime == null) {
                    stopTime = event.getEventTimestamp();
                } else {
                    stopTime = stopTime.after(event.getEventTimestamp()) ? event.getEventTimestamp() : stopTime;
                }
            }
        }
        return stopTime;
    }

    private Date getStackStartTime(List<CloudbreakEvent> stackEvents) {
        Date startDate = null;
        for (CloudbreakEvent event : stackEvents) {
            if (isStartEvent(event)) {
                if (startDate == null) {
                    startDate = event.getEventTimestamp();
                } else {
                    startDate = startDate.before(event.getEventTimestamp()) ? startDate : event.getEventTimestamp();
                }
            }
        }
        return startDate;
    }

    private boolean isStopEvent(CloudbreakEvent event) {
        List<Status> stopStatuses = Arrays.asList(Status.DELETE_IN_PROGRESS, Status.DELETE_FAILED, Status.DELETE_COMPLETED);
        return stopStatuses.contains(Status.valueOf(event.getEventType()));
    }

    private boolean isStartEvent(CloudbreakEvent event) {
        return event.getEventType().equals(Status.AVAILABLE.name());
    }
}
