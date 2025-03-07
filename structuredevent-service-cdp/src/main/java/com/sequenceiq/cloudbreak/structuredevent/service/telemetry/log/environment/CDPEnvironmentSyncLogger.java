package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.environment;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.CDPEnvironmentStructuredSyncEventToCDPEnvironmentSyncConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.CDPTelemetryEventLogger;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Component
public class CDPEnvironmentSyncLogger implements CDPTelemetryEventLogger<CDPEnvironmentStructuredSyncEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPEnvironmentSyncLogger.class);

    @Inject
    private UsageReporter usageReporter;

    @Inject
    private CDPEnvironmentStructuredSyncEventToCDPEnvironmentSyncConverter environmentConverter;

    @Override
    public void log(CDPEnvironmentStructuredSyncEvent cdpEnvironmentStructuredSyncEvent) {
        LOGGER.debug("Sending usage report for Environment");
        usageReporter.cdpEnvironmentSync(environmentConverter.convert(cdpEnvironmentStructuredSyncEvent));
    }
}
