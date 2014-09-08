package com.sequenceiq.cloudbreak.service.events;

import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.Event;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.EventRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;

public class DefaultEventService implements EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEventService.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private EventRepository eventRepository;

    @Override
    public void createStackEvent(Long stackId, String eventType, String eventMessage) {
        LOGGER.debug("Createinf stack event for stackId {}, eventType {}, eventMessage {}", stackId, eventType, eventMessage);
        Stack stack = stackRepository.findById(stackId);

        Event stackEvent = createStackEvent(stack, eventType, eventMessage);

        stackEvent = eventRepository.save(stackEvent);

        LOGGER.debug("Stack event saved: {}", stackEvent);


    }

    private Event createStackEvent(Stack stack, String eventType, String eventMessage) {
        Event stackEvent = new Event();
        stackEvent.setEventTimestamp(Calendar.getInstance().getTimeInMillis());
        stackEvent.setEventMessage(eventMessage);
        stackEvent.setEventType(eventType);

        stackEvent.setAccountId(stack.getUser().getAccount().getId());
        stackEvent.setAccountName(stack.getUser().getAccount().getName());
        stackEvent.setBlueprintId(stack.getCluster().getBlueprint().getId());
        stackEvent.setBlueprintName(stack.getCluster().getBlueprint().getBlueprintName());
        stackEvent.setCloud(stack.getTemplate().cloudPlatform().name());

        stackEvent.setRegion(getRegion(stack));
        stackEvent.setUserId(stack.getUser().getId());
        stackEvent.setUserName(stack.getUser().getFirstName() + " " + stack.getUser().getLastName());
        stackEvent.setVmType(getVmType(stack));

        return stackEvent;
    }

    private String getVmType(Stack stack) {
        String vmType = null;
        switch (stack.getTemplate().cloudPlatform()) {
            case AWS:
                vmType = ((AwsTemplate) stack.getTemplate()).getInstanceType().name();
                break;
            case AZURE:
                vmType = ((AzureTemplate) stack.getTemplate()).getVmType();
                break;
            default:
                throw new IllegalStateException("Unsupported cloud platform :" + stack.getTemplate().cloudPlatform());
        }
        return vmType;
    }

    private String getRegion(Stack stack) {
        String region = null;
        switch (stack.getTemplate().cloudPlatform()) {
            case AWS:
                region = ((AwsTemplate) stack.getTemplate()).getRegion().getName();
                break;
            case AZURE:
                region = ((AzureTemplate) stack.getTemplate()).getLocation().location();
                break;
            default:
                throw new IllegalStateException("Unsupported cloud platform :" + stack.getTemplate().cloudPlatform());
        }
        return region;
    }

    @Override
    public void createClusterEvent(Long stackId, String eventType, String eventMessage) {

    }
}
