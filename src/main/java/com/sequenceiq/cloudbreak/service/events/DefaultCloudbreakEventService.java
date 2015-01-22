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
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
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
    public CloudbreakEvent createStackEvent(Long stackId, String eventType, String eventMessage, InstanceGroup instanceGroup) {
        Stack stack = stackRepository.findById(stackId);
        CloudbreakEvent stackEvent = createStackEvent(stack, eventType, eventMessage, instanceGroup);
        stackEvent = eventRepository.save(stackEvent);

        Notification notification = new Notification(stackEvent);
        notificationSender.send(notification);

        MDCBuilder.buildMdcContext(stackEvent);
        LOGGER.info("Event and notification from the event were created: {}", stackEvent);
        return stackEvent;
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

    private CloudbreakEvent createStackEvent(Stack stack, String eventType, String eventMessage, InstanceGroup instanceGroup) {
        CloudbreakEvent stackEvent = new CloudbreakEvent();

        stackEvent.setEventTimestamp(Calendar.getInstance().getTime());
        stackEvent.setEventMessage(String.format("Interaction on '%s' hostgroup %s", instanceGroup.getGroupName(), eventMessage));
        stackEvent.setEventType(eventType);
        stackEvent.setOwner(stack.getOwner());
        stackEvent.setAccount(stack.getAccount());
        stackEvent.setStackId(stack.getId());
        stackEvent.setStackStatus(stack.getStatus());
        stackEvent.setStackName(stack.getName());
        stackEvent.setNodeCount(stack.getRunningInstanceMetaData().size());

        populateClusterData(stackEvent, stack);
        populateTemplateData(stackEvent, stack, instanceGroup);

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

    private void populateTemplateData(CloudbreakEvent stackEvent, Stack stack, InstanceGroup instanceGroup) {
        stackEvent.setRegion(stack.getRegion());
        stackEvent.setCloud(stack.cloudPlatform().name());
        stackEvent.setInstanceGroup(instanceGroup.getGroupName());
    }
}
