package com.sequenceiq.cloudbreak.service.events;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;

public interface CloudbreakEventService {

    CloudbreakEvent createStackEvent(Long stackId, String eventType, String eventMessage);

    void fireCloudbreakEvent(Long stackId, String eventType, String eventMessage);
}
