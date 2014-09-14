package com.sequenceiq.cloudbreak.service.usages;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository;

@Service
public class DefaultCloudbreakUsageGeneratorService implements CloudbreakUsageGeneratorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudbreakUsagesService.class);

    private static final Map<Long, CloudbreakEvent> RUNNING_STACKS = new HashMap<>();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final String START_TIMES_KEY = "startTimes";
    private static final String END_TIMES_KEY = "endTimes";
    private static final int DAY_IN_HOURS = 24;

    @Autowired
    private CloudbreakUsageRepository usageRepository;

    @Autowired
    private CloudbreakEventRepository eventRepository;

    @Override
    public void generateUserUsage(Long userId) {
        LOGGER.info("Generating user usage for: {}", userId);

        // retrieve all events for the user
        List<CloudbreakEvent> cloudbreakEvents = eventRepository.cloudbreakEvents(userId);

        // split events into usages by day
        Map<String, List<CloudbreakEvent>> eventsByDay = groupEventsByDay(cloudbreakEvents);

        // running stacks are added to the daily usages (to the days following the day it was started)
        processRunningStacks(eventsByDay);

        // calculating running hours for every usage entry
        Map<String, List<CloudbreakUsage>> dailyUsages = calculateRunningHours(eventsByDay);

        LOGGER.debug("usages: {}", cloudbreakEvents);

    }

    private Map<String, List<CloudbreakUsage>> calculateRunningHours(Map<String, List<CloudbreakEvent>> eventsByDay) {

        Map<String, List<CloudbreakUsage>> finalUsagesPerDay = new HashMap<>();

        for (Map.Entry<String, List<CloudbreakEvent>> dailyEventEntry : eventsByDay.entrySet()) {
            // we're within a single day!
            LOGGER.debug("Calculate daily usages for the day: {}", dailyEventEntry.getKey());
            finalUsagesPerDay.put(dailyEventEntry.getKey(), new ArrayList<CloudbreakUsage>());

            Map<String, Map<Long, Date>> dailyEventTimes = processStackEventTimes(dailyEventEntry.getValue());

            for (CloudbreakEvent event : dailyEventEntry.getValue()) {
                long runningMilliseconds = 0;
                long startOfTheDayInMillis = 0;
                try {
                    startOfTheDayInMillis = DATE_FORMAT.parse(dailyEventEntry.getKey()).getTime();
                } catch (ParseException e) {
                    LOGGER.error("Invalid date: {}", dailyEventEntry.getKey());
                    throw new IllegalStateException("Invalid dates found in the process! ");
                }

                long endOfTheDayInMillis = startOfTheDayInMillis + TimeUnit.HOURS.toMillis(DAY_IN_HOURS) - 1;

                // stacks started today, or before!
                if ("AVAILABLE".equals(event.getEventType())) {

                    // started before!
                    if (event.getEventTimestamp().getTime() == 0) {
                        runningMilliseconds = TimeUnit.HOURS.toMillis(DAY_IN_HOURS);

                        //started today
                    } else if (dailyEventTimes.get(START_TIMES_KEY).containsKey(event.getStackId())) {

                        // stopped today
                        if (dailyEventTimes.get(END_TIMES_KEY).containsKey(event.getStackId())) {
                            // difference in milliseconds between start end stop
                            runningMilliseconds = dailyEventTimes.get(END_TIMES_KEY).get(event.getStackId()).getTime()
                                    - dailyEventTimes.get(START_TIMES_KEY).get(event.getStackId()).getTime();
                        } else {
                            // started this day and not stopped
                            // difference in millis between start and end of the day
                            runningMilliseconds = endOfTheDayInMillis - dailyEventTimes.get(START_TIMES_KEY).get(event.getStackId()).getTime();
                        }
                    }
                } else {
                    // went in unavailable state today

                    // has been started today
                    if (dailyEventTimes.get(START_TIMES_KEY).containsKey(event.getStackId())) {
                        LOGGER.debug("Event already handled. Event id: {}, stack id: {}", event.getId(), event.getStackId());
                        // handled by the start event, nothing to do
                    } else if (dailyEventTimes.get(END_TIMES_KEY).containsKey(event.getStackId())) {
                        //has been started earlier
                        runningMilliseconds = dailyEventTimes.get(END_TIMES_KEY).get(event.getStackId()).getTime() - startOfTheDayInMillis;
                    } else {
                        LOGGER.error("Event timestamp not processed properly! Day: {}, eventId {}, stackId {}",
                                dailyEventEntry.getKey(), event.getId(), event.getStackId());
                        throw new IllegalStateException("Invalid event timestamp handling event id: " + event.getId() + " stack id: " + event.getStackId());
                    }
                }

                if (runningMilliseconds > 0) {
                    CloudbreakUsage finalUsage = usageFromEvent(event);
                    finalUsage.setDay(new Date(startOfTheDayInMillis));
                    finalUsage.setRunningHours(String.valueOf(TimeUnit.MILLISECONDS.toHours(runningMilliseconds)));
                    finalUsagesPerDay.get(dailyEventEntry.getKey()).add(finalUsage);
                }
            }
        }
        return finalUsagesPerDay;
    }

    private Map<String, Map<Long, Date>> processStackEventTimes(List<CloudbreakEvent> dailyEvents) {
        LOGGER.debug("Processing stack start and stop dates for daily events: {}", dailyEvents);
        Map<String, Map<Long, Date>> dailyStackEventTimes = new HashMap<>();

        dailyStackEventTimes.put(START_TIMES_KEY, new HashMap<Long, Date>());
        dailyStackEventTimes.put(END_TIMES_KEY, new HashMap<Long, Date>());

        for (CloudbreakEvent event : dailyEvents) {
            if ("AVAILABLE".equals(event.getEventType())) {
                // placeholder events need not to be added!
                if (event.getEventTimestamp().getTime() != 0) {
                    dailyStackEventTimes.get(START_TIMES_KEY).put(event.getStackId(), event.getEventTimestamp());
                }
            } else {
                dailyStackEventTimes.get(END_TIMES_KEY).put(event.getStackId(), event.getEventTimestamp());
            }
        }
        return dailyStackEventTimes;
    }


    private Map<String, List<CloudbreakEvent>> groupEventsByDay(List<CloudbreakEvent> cloudbreakEvents) {
        Map<String, List<CloudbreakEvent>> dayToUsages = new HashMap<>();

        for (CloudbreakEvent cbEvent : cloudbreakEvents) {

            if ("AVAILABLE".equals(cbEvent.getEventType())) {
                if (!RUNNING_STACKS.containsKey(cbEvent.getStackId())) {
                    LOGGER.debug("Stack with id {} became available, adding it to the daily usage", cbEvent.getStackId());
                    RUNNING_STACKS.put(cbEvent.getStackId(), cbEvent);
                    updateDayToUsage(cbEvent, dayToUsages);
                } else {
                    LOGGER.debug("Stack with id {} is already available! skipping event ...", cbEvent.getStackId());
                }
            } else {
                if (RUNNING_STACKS.containsKey(cbEvent.getStackId())) {
                    LOGGER.debug("Stack with id {} became unavailable", cbEvent.getStackId());
                    updateDayToUsage(cbEvent, dayToUsages);
                    RUNNING_STACKS.remove(cbEvent.getStackId());
                } else {
                    LOGGER.debug("Stack with id {} hasn't been in AVAILABLE state. skipping event", cbEvent.getStackId());
                }
            }
        }
        return dayToUsages;
    }

    private void processRunningStacks(Map<String, List<CloudbreakEvent>> eventsByDay) {
        for (String day : eventsByDay.keySet()) {
            // running stacks are those in available state (based on registered events)
            for (Map.Entry<Long, CloudbreakEvent> runningStackEntry : RUNNING_STACKS.entrySet()) {
                Date stackDay = runningStackEntry.getValue().getEventTimestamp();
                try {
                    if (DATE_FORMAT.parse(day).after(stackDay)) {
                        LOGGER.debug("Adding usage for running stack id {}, stack started on {}, day: {}", runningStackEntry.getKey(),
                                stackDay, day);
                        // placeholder events for 24 hour running stacks
                        runningStackEntry.getValue().setEventTimestamp(new Date(0));
                        eventsByDay.get(day).add(runningStackEntry.getValue());
                    }
                } catch (ParseException e) {
                    LOGGER.error("invalid date: {} or {}", day, stackDay);
                }
            }
        }
    }

    private void updateDayToUsage(CloudbreakEvent cbEvent, Map<String, List<CloudbreakEvent>> map) {
        String day = DATE_FORMAT.format(cbEvent.getEventTimestamp());
        if (!map.containsKey(day)) {
            LOGGER.debug("Adding new day to the daily usage map. Day: {}", day);
            map.put(day, new ArrayList<CloudbreakEvent>());
        }
        LOGGER.debug("Adding new usage to the daily usage. Day: {}", day);
        map.get(day).add(cbEvent);
    }

    private void persistUsages(Map<String, List<CloudbreakUsage>> usagesMap) {
        for (Map.Entry<String, List<CloudbreakUsage>> entry : usagesMap.entrySet()) {
            LOGGER.info("Persisting usages: {}", entry.getKey());
            Iterable<CloudbreakUsage> persistedUsageList = usageRepository.save(entry.getValue());
        }
    }

    private CloudbreakUsage usageFromEvent(CloudbreakEvent cbEvent) {
        CloudbreakUsage usage = new CloudbreakUsage();

        usage.setUserId(cbEvent.getUserId());
        usage.setUserName(cbEvent.getUserName());

        usage.setAccountId(cbEvent.getAccountId());
        usage.setAccountName(cbEvent.getAccountName());

        usage.setBlueprintId(cbEvent.getBlueprintId());
        usage.setBlueprintName(cbEvent.getBlueprintName());

        usage.setCloud(cbEvent.getCloud());
        usage.setZone(cbEvent.getRegion());
        usage.setMachineType(cbEvent.getVmType());
        return usage;
    }
}
