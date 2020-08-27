package com.sequenceiq.environment.events;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.FLOW;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPStructuredFlowEventFactory;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.ha.NodeConfig;

@Component
public class EnvironmentStructuredFlowEventFactory implements CDPStructuredFlowEventFactory {

    @Inject
    private Clock clock;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private NodeConfig nodeConfig;

    @Value("${info.app.version:}")
    private String cbVersion;

    @Override
    public CDPStructuredFlowEvent createStructuredFlowEvent(Long resourceId, FlowDetails flowDetails, Boolean detailed) {
        Optional<Environment> environmentOptional = environmentService.findEnvironmentById(resourceId);
        Environment environment = environmentOptional.orElseThrow(notFound("Environment", resourceId));
        String resourceType = CDPStructuredFlowEvent.class.getSimpleName();
        CDPOperationDetails operationDetails = new CDPOperationDetails(clock.getCurrentTimeMillis(), FLOW, resourceType, environment.getId(),
                environment.getName(), nodeConfig.getId(), cbVersion, environment.getAccountId(), environment.getResourceCrn(), environment.getCreator(),
                environment.getResourceCrn(), null);
        return new CDPStructuredFlowEvent(resourceType, operationDetails, flowDetails, null);
    }

    @Override
    public CDPStructuredFlowEvent createStructuredFlowEvent(Long resourceId, FlowDetails flowDetails, Boolean detailed, Exception exception) {
        return null;
    }

    @Override
    public CDPStructuredNotificationEvent createStructuredNotificationEvent(Long resourceId, String notificationType, String message, String groupName) {
        return null;
    }
}
