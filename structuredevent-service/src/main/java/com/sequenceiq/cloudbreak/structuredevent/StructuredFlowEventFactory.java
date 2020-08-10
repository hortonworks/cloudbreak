package com.sequenceiq.cloudbreak.structuredevent;

import java.util.Optional;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.common.api.util.ProvisionEntity;
import com.sequenceiq.flow.ha.NodeConfig;

public interface StructuredFlowEventFactory<E extends ProvisionEntity> {

    StructuredFlowEvent
        createStucturedFlowEvent(Long resourceId, FlowDetails flowDetails, Boolean detailed);

    StructuredFlowEvent
        createStucturedFlowEvent(Long resourceId, FlowDetails flowDetails, Boolean detailed, Exception exception);

    StructuredNotificationEvent
        createStructuredNotificationEvent(E provisionEntity, String notificationType, String message, Optional<String> groupName);

    Clock clock();

    String cbVersion();

    NodeConfig nodeConfig();
}
