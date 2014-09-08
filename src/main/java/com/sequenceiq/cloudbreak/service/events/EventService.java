package com.sequenceiq.cloudbreak.service.events;

public interface EventService {

    void createStackEvent(Long stackId, String eventType, String eventMessage);

    void createClusterEvent(Long stackId, String eventType, String eventMessage);
}
