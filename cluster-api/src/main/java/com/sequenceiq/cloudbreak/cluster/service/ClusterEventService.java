package com.sequenceiq.cloudbreak.cluster.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;

public interface ClusterEventService {

    void fireClusterManagerEvent(Stack stack, ResourceEvent resourceEvent, String eventName, Optional<BigDecimal> clusterManagerEventId);

    void fireCloudbreakEvent(Stack stack, ResourceEvent resourceEvent, Collection<String> eventMessageArgs);
}
