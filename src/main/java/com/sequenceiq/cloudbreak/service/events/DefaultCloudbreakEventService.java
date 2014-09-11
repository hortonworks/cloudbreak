package com.sequenceiq.cloudbreak.service.events;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventSpecifications;
import com.sequenceiq.cloudbreak.repository.StackRepository;

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

    @Override
    public void fireCloudbreakEvent(Long stackId, String eventType, String eventMessage) {
        CloudbreakEventData eventData = new CloudbreakEventData(stackId, eventType, eventMessage);
        LOGGER.info("Fireing cloudbreak event: {}", eventData);
        Event reactorEvent = Event.wrap(eventData);
        reactor.notify(ReactorConfig.CLOUDBREAK_EVENT, reactorEvent);
    }

    @Override
    public CloudbreakEvent createStackEvent(Long stackId, String eventType, String eventMessage) {
        LOGGER.debug("Create stack event for stackId {}, eventType {}, eventMessage {}", stackId, eventType, eventMessage);
        Stack stack = stackRepository.findById(stackId);
        CloudbreakEvent stackEvent = createStackEvent(stack, eventType, eventMessage);
        stackEvent = eventRepository.save(stackEvent);
        LOGGER.debug("Stack event saved: {}", stackEvent);
        return stackEvent;
    }

    @Override
    public List<CloudbreakEvent> cloudbreakEvents(Long userId, Long since) {
        List<CloudbreakEvent> events = null;
        if (null == since) {
            events = eventRepository.findAll(CloudbreakEventSpecifications.eventsForUser(userId));
        } else {
            Date sinceDate = new Date(since);
            events = eventRepository.findAll(Specifications
                    .where(CloudbreakEventSpecifications.eventsForUser(userId))
                    .and(CloudbreakEventSpecifications.eventsSince(since)));
        }
        return null != events ? events : Collections.EMPTY_LIST;
    }

    private CloudbreakEvent createStackEvent(Stack stack, String eventType, String eventMessage) {
        CloudbreakEvent stackEvent = new CloudbreakEvent();

        stackEvent.setStackId(stack.getId());

        stackEvent.setEventTimestamp(Calendar.getInstance().getTime());
        stackEvent.setEventMessage(eventMessage);
        stackEvent.setEventType(eventType);

        stackEvent.setUserId(1L);
        stackEvent.setUserName(stack.getOwner());

        stackEvent.setAccountId(1L);
        stackEvent.setAccountName(stack.getAccount());

        populateClusterData(stackEvent, stack);
        populateTemplateData(stackEvent, stack);

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

    private void populateTemplateData(CloudbreakEvent stackEvent, Stack stack) {
        String vmType = null;
        String region = null;
        switch (stack.getTemplate().cloudPlatform()) {
            case AWS:
                vmType = ((AwsTemplate) stack.getTemplate()).getInstanceType().name();
                region = ((AwsTemplate) stack.getTemplate()).getRegion().getName();
                break;
            case AZURE:
                vmType = ((AzureTemplate) stack.getTemplate()).getVmType();
                region = ((AzureTemplate) stack.getTemplate()).getLocation().location();
                break;
            default:
                throw new IllegalStateException("Unsupported cloud platform :" + stack.getTemplate().cloudPlatform());
        }
        stackEvent.setVmType(vmType);
        stackEvent.setRegion(region);
        stackEvent.setCloud(stack.getTemplate().cloudPlatform().name());
    }
}
