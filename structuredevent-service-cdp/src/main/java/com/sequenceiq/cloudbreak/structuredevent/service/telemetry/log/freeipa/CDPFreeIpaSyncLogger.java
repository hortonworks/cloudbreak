package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.freeipa;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.CDPFreeipaStructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.CDPFreeipaStructuredSyncEventToCDPFreeIPASyncConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.CDPTelemetryEventLogger;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Component
public class CDPFreeIpaSyncLogger implements CDPTelemetryEventLogger<CDPFreeipaStructuredSyncEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPFreeIpaSyncLogger.class);

    @Inject
    private UsageReporter usageReporter;

    @Inject
    private CDPFreeipaStructuredSyncEventToCDPFreeIPASyncConverter freeipaConverter;

    @Override
    public void log(CDPFreeipaStructuredSyncEvent cdpStructuredEvent) {
        LOGGER.debug("Sending usage report for FreeIPA");
        usageReporter.cdpFreeipaSync(freeipaConverter.convert(cdpStructuredEvent));
    }
}
