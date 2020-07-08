package com.sequenceiq.cloudbreak.service.decorator.responseprovider;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StackResponseEntries;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.facade.CloudbreakEventsFacade;

@Component
public class StackResponseEventProvider implements ResponseProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackResponseEventProvider.class);

    @Inject
    private CloudbreakEventsFacade cloudbreakEventsFacade;

    @Override
    public StackV4Response providerEntriesToStackResponse(Stack stack, StackV4Response stackResponse) {
        List<CloudbreakEventV4Response> events = new ArrayList<>();
        List<CloudbreakEventV4Response> cloudbreakEvents = cloudbreakEventsFacade
                .retrieveEventsByStack(stack.getId(), stack.getType(), PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        events.addAll(cloudbreakEvents);
        events.addAll(getLegacyStackType(stack));
        stackResponse.setCloudbreakEvents(events);
        return stackResponse;
    }

    private List<CloudbreakEventV4Response> getLegacyStackType(Stack stack) {
        List<CloudbreakEventV4Response> events = cloudbreakEventsFacade
                .retrieveEventsByStack(stack.getId(), StackType.LEGACY, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        if (events.isEmpty()) {
            LOGGER.info("Cannot find any legacy events for stack: {}, crn: {}", stack.getId(), stack.getResourceCrn());
        } else {
            LOGGER.info("{} legacy events for stack: {}, crn: {}", events.size(), stack.getId(), stack.getResourceCrn());
        }
        return events;
    }

    @Override
    public String type() {
        return StackResponseEntries.EVENTS.getEntryName();
    }
}
