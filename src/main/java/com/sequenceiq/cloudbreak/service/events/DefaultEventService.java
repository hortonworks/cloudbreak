package com.sequenceiq.cloudbreak.service.events;

import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.EventRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;

import reactor.core.Reactor;
import reactor.event.Event;

@Service
public class DefaultEventService implements EventService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEventService.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private Reactor reactor;

    @Override
    public CloudbreakEvent createStackEvent(Long stackId, String eventType, String eventMessage) {
        LOGGER.debug("Create stack event for stackId {}, eventType {}, eventMessage {}", stackId, eventType, eventMessage);
        Stack stack = stackRepository.findById(stackId);
        CloudbreakEvent stackEvent = createStackEvent(stack, eventType, eventMessage);
        stackEvent = eventRepository.save(stackEvent);
        LOGGER.debug("Stack event saved: {}", stackEvent);
        return stackEvent;
    }

    private CloudbreakEvent createStackEvent(Stack stack, String eventType, String eventMessage) {
        CloudbreakEvent stackEvent = new CloudbreakEvent();

        stackEvent.setEventTimestamp(Calendar.getInstance().getTimeInMillis());
        stackEvent.setEventMessage(eventMessage);
        stackEvent.setEventType(eventType);

        stackEvent.setUserId(stack.getUser().getId());
        stackEvent.setUserName(stack.getUser().getFirstName() + " " + stack.getUser().getLastName());

        stackEvent.setAccountId(stack.getUser().getAccount().getId());
        stackEvent.setAccountName(stack.getUser().getAccount().getName());

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


    @Override
    public com.sequenceiq.cloudbreak.domain.CloudbreakEvent createClusterEvent(Long stackId, String eventType, String eventMessage) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void fireCloudbreakEvent(Long stackId, String eventType, String eventMessage) {
        CloudbreakEventData eventData = new CloudbreakEventData(stackId, eventType, eventMessage);
        Event reactorEvent = Event.wrap(eventData);
        reactor.notify(ReactorConfig.CLOUDBREAK_EVENT, reactorEvent);
    }
}
