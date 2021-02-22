package com.sequenceiq.cloudbreak.structuredevent;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.SYNC;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.SyncDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;
import com.sequenceiq.flow.ha.NodeConfig;

@Component
@Transactional
public class StructuredSyncEventFactory {

    @Inject
    private Clock clock;

    @Inject
    private ConversionService conversionService;

    @Inject
    private StackService stackService;

    @Inject
    private NodeConfig nodeConfig;

    @Value("${info.app.version:}")
    private String serviceVersion;

    public StructuredSyncEvent createStructuredSyncEvent(Long resourceId) {
        Stack stack = stackService.getByIdWithTransaction(resourceId);
        String resourceType = (stack.getType() == null || stack.getType().equals(StackType.WORKLOAD))
                ? CloudbreakEventService.DATAHUB_RESOURCE_TYPE
                : CloudbreakEventService.DATALAKE_RESOURCE_TYPE;
        OperationDetails operationDetails = new OperationDetails(clock.getCurrentTimeMillis(), SYNC, resourceType, stack.getId(),
                stack.getName(), nodeConfig.getId(), serviceVersion, stack.getWorkspace().getId(), stack.getCreator().getUserId(),
                stack.getCreator().getUserName(), stack.getTenant().getName(), stack.getResourceCrn(), stack.getCreator().getUserCrn(),
                stack.getEnvironmentCrn(), null);
        SyncDetails syncDetails = conversionService.convert(stack, SyncDetails.class);
        return new StructuredSyncEvent(operationDetails, syncDetails);
    }
}
