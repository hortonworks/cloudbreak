package com.sequenceiq.cloudbreak.service.usages;

import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.BillingStatus;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository;

@Component
public class StackUsageGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackUsageGenerator.class);

    @Inject
    private CloudbreakEventRepository eventRepository;

    @Inject
    private IntervalStackUsageGenerator intervalUsageGenerator;

    public List<CloudbreakUsage> generate(List<CloudbreakEvent> stackEvents) {
        List<CloudbreakUsage> stackUsages = new LinkedList<>();
        CloudbreakEvent actEvent = null;
        try {
            CloudbreakEvent start = null;
            for (CloudbreakEvent cbEvent : stackEvents) {
                actEvent = cbEvent;
                if (isStartEvent(cbEvent) && start == null) {
                    start = cbEvent;
                } else if (validStopEvent(start, cbEvent)) {
                    addAllGeneratedUsages(stackUsages, start, cbEvent.getEventTimestamp());
                    start = null;
                }
            }

            generateRunningStackUsage(stackUsages, start);
        } catch (Exception e) {
            LOGGER.error("Usage generation failed for stack(id:{})! Error when processing event(id:{})! Ex: {}", actEvent.getStackId(), actEvent.getId(), e);
            throw new IllegalStateException(e);
        }
        return stackUsages;
    }

    private boolean validStopEvent(CloudbreakEvent start, CloudbreakEvent cbEvent) {
        return start != null && start.getEventTimestamp().before(cbEvent.getEventTimestamp()) && isStopEvent(cbEvent);
    }

    private boolean isStopEvent(CloudbreakEvent event) {
        return BillingStatus.BILLING_STOPPED.name().equals(event.getEventType());
    }

    private boolean isStartEvent(CloudbreakEvent event) {
        return BillingStatus.BILLING_STARTED.name().equals(event.getEventType());
    }

    private void addAllGeneratedUsages(List<CloudbreakUsage> stackUsages, CloudbreakEvent startEvent, Date stopTime) throws ParseException {
        List<CloudbreakUsage> usages = intervalUsageGenerator.generateUsages(startEvent.getEventTimestamp(), stopTime, startEvent);
        stackUsages.addAll(usages);
    }

    private void generateRunningStackUsage(List<CloudbreakUsage> dailyCbUsages, CloudbreakEvent startEvent) throws ParseException {
        if (startEvent != null) {
            Calendar cal = Calendar.getInstance();
            setDayToBeginning(cal);
            addAllGeneratedUsages(dailyCbUsages, startEvent, cal.getTime());

            //get overflowed minutes from the start event
            Calendar start = Calendar.getInstance();
            start.setTime(startEvent.getEventTimestamp());
            cal.set(MINUTE, start.get(MINUTE));
            //saveOne billing start event for daily usage generation
            CloudbreakEvent newBillingStart = createBillingStarterCloudbreakEvent(startEvent, cal);
            eventRepository.save(newBillingStart);
            LOGGER.debug("BILLING_STARTED is created with date:{} for running stack {}.", cal.getTime(), newBillingStart.getStackId());
        }
    }

    private void setDayToBeginning(Calendar calendar) {
        calendar.set(HOUR_OF_DAY, 0);
        calendar.set(MINUTE, 0);
        calendar.set(SECOND, 0);
        calendar.set(MILLISECOND, 0);
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
        event.setCloud(startEvent.getCloud());
        event.setRegion(startEvent.getRegion());
        event.setAvailabilityZone(startEvent.getAvailabilityZone());
        event.setStackId(startEvent.getStackId());
        event.setStackStatus(startEvent.getStackStatus());
        event.setStackName(startEvent.getStackName());
        event.setNodeCount(startEvent.getNodeCount());
        event.setInstanceGroup(startEvent.getInstanceGroup());
        return event;
    }
}
