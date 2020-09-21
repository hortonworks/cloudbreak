package com.sequenceiq.freeipa.events;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.FLOW;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPStructuredFlowEventFactory;
import com.sequenceiq.flow.ha.NodeConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeipaStructuredFlowEventFactory implements CDPStructuredFlowEventFactory {

    @Inject
    private Clock clock;

    @Inject
    private StackService stackService;

    @Inject
    private NodeConfig nodeConfig;

    @Value("${info.app.version:}")
    private String serviceVersion;

    @Override
    public CDPStructuredFlowEvent createStructuredFlowEvent(Long resourceId, FlowDetails flowDetails, Boolean detailed) {
        return createStructuredFlowEvent(resourceId, flowDetails, detailed, null);
    }

    @Override
    public CDPStructuredFlowEvent createStructuredFlowEvent(Long resourceId, FlowDetails flowDetails, Boolean detailed, Exception exception) {
        Stack stack = stackService.getStackById(resourceId);
        String resourceType = CloudbreakEventService.FREEIPA_RESOURCE_TYPE;
        CDPOperationDetails operationDetails = new CDPOperationDetails(clock.getCurrentTimeMillis(), FLOW, resourceType, stack.getId(),
                stack.getName(), nodeConfig.getId(), serviceVersion, stack.getAccountId(), stack.getResourceCrn(), ThreadBasedUserCrnProvider.getUserCrn(),
                stack.getEnvironmentCrn(), null);
        CDPStructuredFlowEvent event = new CDPStructuredFlowEvent(operationDetails, flowDetails, null);
        if (exception != null) {
            event.setException(ExceptionUtils.getStackTrace(exception));
        }
        return event;
    }
}
