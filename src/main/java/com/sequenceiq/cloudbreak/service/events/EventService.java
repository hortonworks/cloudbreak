package com.sequenceiq.cloudbreak.service.events;

import com.sequenceiq.cloudbreak.domain.Event;

public interface EventService {

    Event createStackEvent(Long stackId, String eventType, String eventMessage);

    Event createClusterEvent(Long stackId, String eventType, String eventMessage);
}
