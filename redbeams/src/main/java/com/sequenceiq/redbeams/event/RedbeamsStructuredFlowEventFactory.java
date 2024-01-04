package com.sequenceiq.redbeams.event;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.FLOW;

import jakarta.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.redbeams.CDPRedbeamsStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.redbeams.RedbeamsDetails;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPStructuredFlowEventFactory;
import com.sequenceiq.flow.ha.NodeConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Component
public class RedbeamsStructuredFlowEventFactory implements CDPStructuredFlowEventFactory {

    @Inject
    private Clock clock;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private NodeConfig nodeConfig;

    @Value("${info.app.version:}")
    private String serviceVersion;

    @Override
    public CDPStructuredFlowEvent createStructuredFlowEvent(Long resourceId, FlowDetails flowDetails) {
        return createStructuredFlowEvent(resourceId, flowDetails, null);
    }

    @Override
    public CDPStructuredFlowEvent createStructuredFlowEvent(Long resourceId, FlowDetails flowDetails, Exception exception) {
        DBStack dbStack = dbStackService.getById(resourceId);
        CDPOperationDetails operationDetails = new CDPOperationDetails(
                clock.getCurrentTimeMillis(),
                FLOW,
                CloudbreakEventService.REDBEAMS_RESOURCE_TYPE,
                dbStack.getId(),
                dbStack.getName(),
                nodeConfig.getId(),
                serviceVersion,
                dbStack.getAccountId(),
                dbStack.getResourceCrn(),
                ThreadBasedUserCrnProvider.getUserCrn(),
                dbStack.getEnvironmentId(),
                null);

        RedbeamsDetails redbeamsDetails = new RedbeamsDetails(
                dbStack.getId(),
                dbStack.getName(),
                dbStack.getResourceCrn(),
                dbStack.getAccountId(),
                dbStack.getEnvironmentId(),
                dbStack.getRegion(),
                dbStack.getAvailabilityZone());
        CDPRedbeamsStructuredFlowEvent event = new CDPRedbeamsStructuredFlowEvent(operationDetails, flowDetails, redbeamsDetails,
                null, null);
        if (exception != null) {
            event.setException(ExceptionUtils.getStackTrace(exception));
        }
        return event;
    }
}
