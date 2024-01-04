package com.sequenceiq.freeipa.events;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.FLOW;

import jakarta.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.CDPFreeIpaStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPStructuredFlowEventFactory;
import com.sequenceiq.flow.ha.NodeConfig;
import com.sequenceiq.freeipa.converter.stack.StackToStackDetailsConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeipaStructuredFlowEventFactory implements CDPStructuredFlowEventFactory {

    @Inject
    private Clock clock;

    @Inject
    private StackService stackService;

    @Inject
    private NodeConfig nodeConfig;

    @Inject
    private StackToStackDetailsConverter stackToStackDetailsConverter;

    @Value("${info.app.version:}")
    private String serviceVersion;

    @Override
    public CDPStructuredFlowEvent createStructuredFlowEvent(Long resourceId, FlowDetails flowDetails) {
        return createStructuredFlowEvent(resourceId, flowDetails, null);
    }

    @Override
    public CDPStructuredFlowEvent createStructuredFlowEvent(Long resourceId, FlowDetails flowDetails, Exception exception) {
        Stack stack = stackService.getStackById(resourceId);
        String resourceType = CloudbreakEventService.FREEIPA_RESOURCE_TYPE;
        CDPOperationDetails operationDetails = new CDPOperationDetails(clock.getCurrentTimeMillis(), FLOW, resourceType, stack.getId(),
                stack.getName(), nodeConfig.getId(), serviceVersion, stack.getAccountId(), stack.getResourceCrn(), ThreadBasedUserCrnProvider.getUserCrn(),
                stack.getEnvironmentCrn(), null);

        StackDetails stackDetails = stackToStackDetailsConverter.convert(stack);

        StackStatus stackStatus = stack.getStackStatus();
        CDPFreeIpaStructuredFlowEvent event = new CDPFreeIpaStructuredFlowEvent(operationDetails, flowDetails, stackDetails,
                stackStatus.getDetailedStackStatus().name(), stackStatus.getStatusReason());
        if (exception != null) {
            event.setException(ExceptionUtils.getStackTrace(exception));
        }
        return event;
    }
}
