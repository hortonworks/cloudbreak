package com.sequenceiq.cloudbreak.service.events;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventSpecifications;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;

import reactor.core.Reactor;
import reactor.event.Event;

@Service
public class DefaultCloudbreakEventService implements CloudbreakEventService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudbreakEventService.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private CloudbreakEventRepository eventRepository;

    @Autowired
    private Reactor reactor;

    @Autowired
    private NotificationSender notificationSender;

    @Override
    public void fireCloudbreakEvent(Long stackId, String eventType, String eventMessage) {
        CloudbreakEventData eventData = new CloudbreakEventData(stackId, eventType, eventMessage);
        MDCBuilder.buildMdcContext(eventData);
        LOGGER.info("Fireing cloudbreak event: {}", eventData);
        Event reactorEvent = Event.wrap(eventData);
        reactor.notify(ReactorConfig.CLOUDBREAK_EVENT, reactorEvent);
    }

    @Override
    public void fireCloudbreakInstanceGroupEvent(Long stackId, String eventType, String eventMessage, String instanceGroupName) {
        InstanceGroupEventData eventData = new InstanceGroupEventData(stackId, eventType, eventMessage, instanceGroupName);
        MDCBuilder.buildMdcContext(eventData);
        LOGGER.info("Fireing cloudbreak event: {}", eventData);
        Event reactorEvent = Event.wrap(eventData);
        reactor.notify(ReactorConfig.CLOUDBREAK_EVENT, reactorEvent);
    }

    @Override
    public CloudbreakEvent createStackEvent(CloudbreakEventData eventData) {
        String instanceGroupName = getInstanceGroupNameFromEvent(eventData);
        Stack stack = stackRepository.findById(eventData.getEntityId());
        CloudbreakEvent stackEvent = createStackEvent(stack, eventData.getEventType(), eventData.getEventMessage(), instanceGroupName);
        stackEvent = eventRepository.save(stackEvent);

        Notification notification = new Notification(stackEvent);
        notificationSender.send(notification);

        MDCBuilder.buildMdcContext(stackEvent);
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
//            String msg = stackEvent.getEventMessage();
//            stackEvent.setEventMessage(String.format("Interaction on '%s' hostgroup: %s", instanceGroupName, msg));
            stackEvent.setInstanceGroup(instanceGroupName);
        }

        return stackEvent;
    }

    private void populateClusterData(CloudbreakEvent stackEvent, Stack stack) {
        MDCBuilder.buildMdcContext(stackEvent);
        if (null != stack.getCluster()) {
            stackEvent.setBlueprintId(stack.getCluster().getBlueprint().getId());
            stackEvent.setBlueprintName(stack.getCluster().getBlueprint().getBlueprintName());
        } else {
            LOGGER.debug("No cluster data available for the stack: {}", stack.getId());
        }
    }
}
