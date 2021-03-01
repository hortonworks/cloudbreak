package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

@Component
public class StructuredSyncEventToCDPDatalakeSyncConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredSyncEventToCDPDatalakeSyncConverter.class);

    @Inject
    private StructuredEventToCDPOperationDetailsConverter operationDetailsConverter;

    @Inject
    private StructuredSyncEventToCDPSyncDetailsConverter syncDetailsConverter;

    @Inject
    private StructuredEventToClusterDetailsConverter clusterDetailsConverter;

    @Inject
    private StructuredEventToStatusDetailsConverter statusDetailsConverter;

    public UsageProto.CDPDatalakeSync convert(StructuredSyncEvent structuredSyncEvent) {
        if (structuredSyncEvent == null) {
            return null;
        }

        UsageProto.CDPDatalakeSync.Builder cdpDatalakeSyncBuilder = UsageProto.CDPDatalakeSync.newBuilder();
        cdpDatalakeSyncBuilder.setOperationDetails(operationDetailsConverter.convert(structuredSyncEvent));
        cdpDatalakeSyncBuilder.setSyncDetails(syncDetailsConverter.convert(structuredSyncEvent));
        cdpDatalakeSyncBuilder.setClusterDetails(clusterDetailsConverter.convert(structuredSyncEvent));
        cdpDatalakeSyncBuilder.setStatusDetails(statusDetailsConverter.convert(structuredSyncEvent));

        UsageProto.CDPDatalakeSync ret = cdpDatalakeSyncBuilder.build();
        LOGGER.debug("Converted telemetry event: {}", ret);
        return ret;
    }
}
