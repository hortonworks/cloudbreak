package com.sequenceiq.cloudbreak.cluster.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.event.ResourceEvent;

public interface ClusterEventService {

    void fireClusterManagerEvent(StackDtoDelegate stack, ResourceEvent resourceEvent, String eventName, Optional<BigDecimal> clusterManagerEventId);

    void fireCloudbreakEvent(StackDtoDelegate stack, ResourceEvent resourceEvent, Collection<String> eventMessageArgs);
}
