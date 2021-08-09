package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.StructuredSyncEventToCDPDatahubSyncConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.StructuredSyncEventToCDPDatalakeSyncConverter;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Component
public class ClusterSyncLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSyncLogger.class);

    @Inject
    private UsageReporter usageReporter;

    @Inject
    private StructuredSyncEventToCDPDatalakeSyncConverter datalakeConverter;

    @Inject
    private StructuredSyncEventToCDPDatahubSyncConverter datahubConverter;

    public void log(StructuredSyncEvent structuredSyncEvent) {

        String resourceType = structuredSyncEvent.getOperation().getResourceType();
        if (resourceType != null) {
            switch (resourceType) {
                case CloudbreakEventService.DATALAKE_RESOURCE_TYPE:
                    LOGGER.debug("Sending usage report for {}", resourceType);
                    usageReporter.cdpDatalakeSync(datalakeConverter.convert(structuredSyncEvent));
                    break;
                case CloudbreakEventService.DATAHUB_RESOURCE_TYPE:
                    LOGGER.debug("Sending usage report for {}", resourceType);
                    usageReporter.cdpDatahubSync(datahubConverter.convert(structuredSyncEvent));
                    break;
                default:
                    LOGGER.debug("We are not sending usage report for {}", resourceType);
            }
        }
    }
}
