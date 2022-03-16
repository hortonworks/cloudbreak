package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.environment;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.CDPFreeIpaStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.CDPFreeIpaStructuredFlowEventToCDPFreeIpaStatusChangedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.CDPTelemetryEventLogger;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseMapper;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Component
public class CDPFreeIpaLogger implements CDPTelemetryEventLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPFreeIpaLogger.class);

    @Inject
    private UsageReporter usageReporter;

    @Inject
    private FreeIpaUseCaseMapper freeIpaUseCaseMapper;

    @Inject
    private CDPFreeIpaStructuredFlowEventToCDPFreeIpaStatusChangedConverter statusChangedConverter;

    @Override
    public void log(CDPStructuredFlowEvent cdpStructuredFlowEvent) {

        FlowDetails flow = cdpStructuredFlowEvent.getFlow();
        UsageProto.CDPFreeIPAStatus.Value useCase = freeIpaUseCaseMapper.useCase(flow);
        LOGGER.debug("Telemetry use case: {}", useCase);

        if (cdpStructuredFlowEvent instanceof CDPFreeIpaStructuredFlowEvent &&
                useCase != UsageProto.CDPFreeIPAStatus.Value.UNSET) {
            LOGGER.debug("Sending usage report for {}", cdpStructuredFlowEvent.getOperation().getResourceType());
            usageReporter.cdpFreeIpaStatusChanged(
                    statusChangedConverter.convert((CDPFreeIpaStructuredFlowEvent) cdpStructuredFlowEvent, useCase));
        }
    }
}
