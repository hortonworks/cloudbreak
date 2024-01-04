package com.sequenceiq.cloudbreak.core.flow2.stack;

import java.util.Arrays;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Service
public class CloudbreakFlowMessageService implements FlowMessageService {

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    public void fireEventAndLog(Long stackId, String eventType, ResourceEvent resourceEvent, String... eventMessageArgs) {
        cloudbreakEventService.fireCloudbreakEvent(stackId, eventType, resourceEvent, Arrays.asList(eventMessageArgs));
    }

    public void fireInstanceGroupEventAndLog(Long stackId, String eventType, String instanceGroup, ResourceEvent resourceEvent, String... eventMessageArgs) {
        cloudbreakEventService.fireCloudbreakInstanceGroupEvent(stackId, eventType, instanceGroup, resourceEvent, Arrays.asList(eventMessageArgs));
    }
}
