package com.sequenceiq.freeipa.events.sync;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.SYNC;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.CDPFreeipaStructuredSyncEvent;
import com.sequenceiq.freeipa.converter.stack.StackToStackDetailsConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class StructuredSyncEventFactory {

    @Inject
    private StackService stackService;

    @Inject
    private Clock clock;

    @Inject
    private NodeConfig nodeConfig;

    @Inject
    private StackToStackDetailsConverter stackToStackDetailsConverter;

    @Value("${info.app.version:}")
    private String serviceVersion;

    public CDPFreeipaStructuredSyncEvent createCDPFreeipaStructuredSyncEvent(Long resourceId) {
        Stack stack = stackService.getStackById(resourceId);
        CDPOperationDetails cdpOperationDetails = new CDPOperationDetails(
                clock.getCurrentTimeMillis(),
                SYNC,
                CloudbreakEventService.FREEIPA_RESOURCE_TYPE,
                resourceId,
                stack.getName(),
                nodeConfig.getId(),
                serviceVersion,
                stack.getAccountId(),
                stack.getResourceCrn(),
                ThreadBasedUserCrnProvider.getUserCrn(),
                stack.getEnvironmentCrn(),
                null);

        StackDetails stackDetails = stackToStackDetailsConverter.convert(stack);

        return new CDPFreeipaStructuredSyncEvent(cdpOperationDetails, stackDetails);
    }
}
