package com.sequenceiq.cloudbreak.service.events;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackRepository;

import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class CloudbreakEventHandler implements Consumer<Event<CloudbreakEventData>> {

    @Autowired
    private CloudbreakEventService eventService;
    @Autowired
    private StackRepository stackRepository;

    @Override
    public void accept(Event<CloudbreakEventData> cloudbreakEvent) {
        CloudbreakEventData event = cloudbreakEvent.getData();
        MDCBuilder.buildMdcContext(event);
        Stack stack = stackRepository.findById(event.getEntityId());
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            com.sequenceiq.cloudbreak.domain.CloudbreakEvent registeredEvent =
                    eventService.createStackEvent(event.getEntityId(), event.getEventType(), event.getEventMessage(), instanceGroup);
        }
    }
}
