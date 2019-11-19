package com.sequenceiq.cloudbreak.message;

import com.sequenceiq.cloudbreak.event.ResourceEvent;

public interface FlowMessageService {
    void fireEventAndLog(Long stackId, String eventType, ResourceEvent resourceEvent, String... eventMessageArgs);

    void fireInstanceGroupEventAndLog(Long stackId, String eventType, String instanceGroup, ResourceEvent resourceEvent, String... eventMessageArgs);

}
