package com.sequenceiq.cloudbreak.core.flow2.stack;

import java.util.Arrays;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Service
public class CloudbreakFlowMessageService implements FlowMessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakFlowMessageService.class);

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    public void fireEventAndLog(Long stackId, String eventType, ResourceEvent resourceEvent, String... eventMessageArgs) {
        cloudbreakEventService.fireCloudbreakEvent(stackId, eventType, resourceEvent, Arrays.asList(eventMessageArgs));
    }

    public void fireInstanceGroupEventAndLog(Long stackId, String eventType, String instanceGroup, ResourceEvent resourceEvent, String... eventMessageArgs) {
        cloudbreakEventService.fireCloudbreakInstanceGroupEvent(stackId, eventType, instanceGroup, resourceEvent, Arrays.asList(eventMessageArgs));
    }
}
