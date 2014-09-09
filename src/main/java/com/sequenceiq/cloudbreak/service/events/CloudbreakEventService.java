package com.sequenceiq.cloudbreak.service.events;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;

public interface CloudbreakEventService {

    void fireCloudbreakEvent(Long stackId, String eventType, String eventMessage);

    CloudbreakEvent createStackEvent(Long stackId, String eventType, String eventMessage);

    List<CloudbreakEvent> cloudbreakEvents(Long userId, Long since);
}
