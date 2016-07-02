package com.sequenceiq.cloudbreak.service.events;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventSpecifications;
import com.sequenceiq.cloudbreak.repository.StackRepository;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;

@Service
public class DefaultCloudbreakEventService implements CloudbreakEventService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudbreakEventService.class);
    private static final String CLOUDBREAK_EVENT = "CLOUDBREAK_EVENT";

    @Inject
    private StackRepository stackRepository;

    @Inject
    private CloudbreakEventRepository eventRepository;

    @Inject
    private EventBus reactor;

    @Inject
    private CloudbreakEventHandler cloudbreakEventHandler;

    @PostConstruct
    public void setup() {
        reactor.on(Selectors.$(CLOUDBREAK_EVENT), cloudbreakEventHandler);
    }

    @Override
    public void fireCloudbreakEvent(Long stackId, String eventType, String eventMessage) {
        CloudbreakEventData eventData = new CloudbreakEventData(stackId, eventType, eventMessage);
        LOGGER.info("Firing Cloudbreak event: {}", eventData);
        Event reactorEvent = Event.wrap(eventData);
        reactor.notify(CLOUDBREAK_EVENT, reactorEvent);
    }

    @Override
    public void fireCloudbreakInstanceGroupEvent(Long stackId, String eventType, String eventMessage, String instanceGroupName) {
        InstanceGroupEventData eventData = new InstanceGroupEventData(stackId, eventType, eventMessage, instanceGroupName);
        LOGGER.info("Fireing cloudbreak event: {}", eventData);
        Event reactorEvent = Event.wrap(eventData);
        reactor.notify(CLOUDBREAK_EVENT, reactorEvent);
    }

    @Override
    public CloudbreakEvent createStackEvent(CloudbreakEventData eventData) {
        LOGGER.debug("Creating stack event from: {}", eventData);
        String instanceGroupName = getInstanceGroupNameFromEvent(eventData);
        Stack stack = stackRepository.findById(eventData.getEntityId());
        CloudbreakEvent stackEvent = createStackEvent(stack, eventData.getEventType(), eventData.getEventMessage(), instanceGroupName);
        stackEvent = eventRepository.save(stackEvent);
        LOGGER.info("Created stack event: {}", stackEvent);
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
    @SuppressWarnings("unchecked")
    public List<CloudbreakEvent> cloudbreakEvents(String owner, Long since) {
        List<CloudbreakEvent> events;
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
        stackEvent.setAvailabilityZone(stack.getAvailabilityZone());
        stackEvent.setCloud(stack.cloudPlatform());

        populateClusterData(stackEvent, stack);

        if (instanceGroupName != null) {
            stackEvent.setInstanceGroup(instanceGroupName);
        }

        return stackEvent;
    }

    private void populateClusterData(CloudbreakEvent stackEvent, Stack stack) {
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            stackEvent.setClusterStatus(cluster.getStatus());
            if (cluster.getBlueprint() != null) {
                stackEvent.setBlueprintId(cluster.getBlueprint().getId());
                stackEvent.setBlueprintName(cluster.getBlueprint().getBlueprintName());
            }
            stackEvent.setClusterId(cluster.getId());
            stackEvent.setClusterName(cluster.getName());
        } else {
            LOGGER.debug("No cluster data available for the stack: {}", stack.getId());
        }
    }
}
