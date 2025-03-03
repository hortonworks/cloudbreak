package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.datalake;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.datalake.CDPDatalakeStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.CDPDatalakeStructuredFlowEventToCDPDatalakeStatusChangedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.CDPTelemetryFlowEventLogger;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.DatalakeUseCaseMapper;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Component
public class CDPDatalakeLogger implements CDPTelemetryFlowEventLogger<CDPDatalakeStructuredFlowEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPDatalakeLogger.class);

    @Inject
    private UsageReporter usageReporter;

    @Inject
    private DatalakeUseCaseMapper datalakeUseCaseMapper;

    @Inject
    private CDPDatalakeStructuredFlowEventToCDPDatalakeStatusChangedConverter datalakeStatusChangedConverter;

    @Override
    public void log(CDPDatalakeStructuredFlowEvent cdpStructuredEvent) {
        Value useCase = datalakeUseCaseMapper.useCase(cdpStructuredEvent.getFlow());
        if (useCase != Value.UNSET) {
            LOGGER.debug("Sending usage report for use case {}", useCase);
            usageReporter.cdpDatalakeStatusChanged(datalakeStatusChangedConverter.convert(cdpStructuredEvent, useCase));
        }
    }

    @Override
    public Class<CDPDatalakeStructuredFlowEvent> acceptableEventClass() {
        return CDPDatalakeStructuredFlowEvent.class;
    }
}
