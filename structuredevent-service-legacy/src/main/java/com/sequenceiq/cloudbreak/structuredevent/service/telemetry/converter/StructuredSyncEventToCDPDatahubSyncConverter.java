package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

@Component
public class StructuredSyncEventToCDPDatahubSyncConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredSyncEventToCDPDatahubSyncConverter.class);

    @Inject
    private StructuredSyncEventToCDPOperationDetailsConverter operationDetailsConverter;

    @Inject
    private StructuredSyncEventToCDPSyncDetailsConverter syncDetailsConverter;

    public UsageProto.CDPDatahubSync convert(StructuredSyncEvent structuredSyncEvent) {
        if (structuredSyncEvent == null) {
            return null;
        }

        UsageProto.CDPDatahubSync.Builder cdpDatahubSyncBuilder = UsageProto.CDPDatahubSync.newBuilder();
        cdpDatahubSyncBuilder.setOperationDetails(operationDetailsConverter.convert(structuredSyncEvent));
        cdpDatahubSyncBuilder.setSyncDetails(syncDetailsConverter.convert(structuredSyncEvent));

        UsageProto.CDPDatahubSync ret = cdpDatahubSyncBuilder.build();
        LOGGER.debug("Converted telemetry event: {}", ret);
        return ret;
    }
}
