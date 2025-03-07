package com.sequenceiq.environment.events.sync;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.SYNC;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@Component
public class StructuredSyncEventFactory {

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private Clock clock;

    @Inject
    private NodeConfig nodeConfig;

    @Inject
    private EnvironmentDtoConverter environmentDtoConverter;

    @Value("${info.app.version:}")
    private String serviceVersion;

    public CDPEnvironmentStructuredSyncEvent createCDPEnvironmentStructuredSyncEvent(Long resourceId) {
        Environment environment = environmentService.findEnvironmentByIdOrThrow(resourceId);
        CDPOperationDetails cdpOperationDetails = new CDPOperationDetails(
                clock.getCurrentTimeMillis(),
                SYNC,
                CloudbreakEventService.ENVIRONMENT_RESOURCE_TYPE,
                resourceId,
                environment.getName(),
                nodeConfig.getId(),
                serviceVersion,
                environment.getAccountId(),
                environment.getResourceCrn(),
                ThreadBasedUserCrnProvider.getUserCrn(),
                environment.getResourceCrn(),
                null);

        EnvironmentDetails environmentDetails = environmentDtoConverter.environmentToDto(environment);

        return new CDPEnvironmentStructuredSyncEvent(cdpOperationDetails, environmentDetails);
    }

}
