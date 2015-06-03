package com.sequenceiq.cloudbreak.service.events;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.EventBusConfig;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventSpecifications;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Service
public class DefaultCloudbreakEventService implements CloudbreakEventService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudbreakEventService.class);

    @Inject
    private StackRepository stackRepository;

    @Inject
    private CloudbreakEventRepository eventRepository;

    @Inject
    private EventBus reactor;

    @Inject
    private NotificationSender notificationSender;

    @Override
    public void fireCloudbreakEvent(Long stackId, String eventType, String eventMessage) {
        CloudbreakEventData eventData = new CloudbreakEventData(stackId, eventType, eventMessage);
        LOGGER.info("Firing Cloudbreak event: {}", eventData);
        Event reactorEvent = Event.wrap(eventData);
        reactor.notify(EventBusConfig.CLOUDBREAK_EVENT, reactorEvent);
    }

    @Override
    public void fireCloudbreakInstanceGroupEvent(Long stackId, String eventType, String eventMessage, String instanceGroupName) {
        InstanceGroupEventData eventData = new InstanceGroupEventData(stackId, eventType, eventMessage, instanceGroupName);
        LOGGER.info("Fireing cloudbreak event: {}", eventData);
        Event reactorEvent = Event.wrap(eventData);
        reactor.notify(EventBusConfig.CLOUDBREAK_EVENT, reactorEvent);
    }

    @Override
    public CloudbreakEvent createStackEvent(CloudbreakEventData eventData) {
        String instanceGroupName = getInstanceGroupNameFromEvent(eventData);
        Stack stack = stackRepository.findById(eventData.getEntityId());
        CloudbreakEvent stackEvent = createStackEvent(stack, eventData.getEventType(), eventData.getEventMessage(), instanceGroupName);
        stackEvent = eventRepository.save(stackEvent);

        Notification notification = new Notification(stackEvent);
        notificationSender.send(notification);

        LOGGER.info("Event and notification from the event were created: {}", stackEvent);
        return stackEvent;
    }

    private String getInstanceGroupNameFromEvent(CloudbreakEventData eventData) {
        String instanceGroup = null;
        if (eventData instanceof InstanceGroupEventData) {
            instanceGroup = ((InstanceGroupEventData) eventData).getInstanceGroupName();
        }
        return instanceGroup;
    }

    @Override
    public List<CloudbreakEvent> cloudbreakEvents(String owner, Long since) {
        List<CloudbreakEvent> events = null;
        if (null == since) {
            events = eventRepository.findAll(CloudbreakEventSpecifications.eventsForUser(owner));
        } else {
            events = eventRepository.findAll(Specifications
                    .where(CloudbreakEventSpecifications.eventsForUser(owner))
                    .and(CloudbreakEventSpecifications.eventsSince(since)));
        }
        return null != events ? events : Collections.EMPTY_LIST;
    }

    private CloudbreakEvent createStackEvent(Stack stack, String eventType, String eventMessage, String instanceGroupName) {
        CloudbreakEvent stackEvent = new CloudbreakEvent();

        stackEvent.setEventTimestamp(Calendar.getInstance().getTime());
        stackEvent.setEventMessage(eventMessage);
        stackEvent.setEventType(eventType);
        stackEvent.setOwner(stack.getOwner());
        stackEvent.setAccount(stack.getAccount());
        stackEvent.setStackId(stack.getId());
        stackEvent.setStackStatus(stack.getStatus());
        stackEvent.setStackName(stack.getName());
        stackEvent.setNodeCount(stack.getRunningInstanceMetaData().size());
        stackEvent.setRegion(stack.getRegion());
        stackEvent.setCloud(stack.cloudPlatform().name());

        populateClusterData(stackEvent, stack);

        if (instanceGroupName != null) {
            stackEvent.setInstanceGroup(instanceGroupName);
        }

        return stackEvent;
    }

    private void populateClusterData(CloudbreakEvent stackEvent, Stack stack) {
        if (null != stack.getCluster()) {
            stackEvent.setBlueprintId(stack.getCluster().getBlueprint().getId());
            stackEvent.setBlueprintName(stack.getCluster().getBlueprint().getBlueprintName());
        } else {
            LOGGER.debug("No cluster data available for the stack: {}", stack.getId());
        }
    }
}
