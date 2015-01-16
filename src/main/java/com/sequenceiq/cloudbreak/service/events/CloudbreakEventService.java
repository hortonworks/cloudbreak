package com.sequenceiq.cloudbreak.service.events;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;

public interface CloudbreakEventService {

    void fireCloudbreakEvent(Long stackId, String eventType, String eventMessage);

    CloudbreakEvent createStackEvent(Long stackId, String eventType, String eventMessage, InstanceGroup instanceGroup);

    List<CloudbreakEvent> cloudbreakEvents(String user, Long since);
}
