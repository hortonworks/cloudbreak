package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.environment;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.CDPEnvironmentStructuredFlowEventToCDPEnvironmentRequestedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.CDPEnvironmentStructuredFlowEventToCDPEnvironmentStatusChangedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.CDPTelemetryEventLogger;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseMapper;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Component
public class CDPEnvironmentLogger implements CDPTelemetryEventLogger<CDPEnvironmentStructuredFlowEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPEnvironmentLogger.class);

    @Inject
    private UsageReporter usageReporter;

    @Inject
    private EnvironmentUseCaseMapper environmentUseCaseMapper;

    @Inject
    private CDPEnvironmentStructuredFlowEventToCDPEnvironmentRequestedConverter environmentRequestedConverter;

    @Inject
    private CDPEnvironmentStructuredFlowEventToCDPEnvironmentStatusChangedConverter statusChangedConverter;

    @Override
    public void log(CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent) {
        Value useCase = environmentUseCaseMapper.useCase(cdpStructuredFlowEvent.getFlow());
        if (useCase != Value.UNSET) {
            LOGGER.debug("Sending usage report for {} for use case {}", cdpStructuredFlowEvent.getOperation().getResourceType(), useCase);
            usageReporter.cdpEnvironmentStatusChanged(statusChangedConverter.convert((CDPEnvironmentStructuredFlowEvent) cdpStructuredFlowEvent, useCase));
        }
    }

    @Override
    public Class<CDPEnvironmentStructuredFlowEvent> acceptableEventClass() {
        return CDPEnvironmentStructuredFlowEvent.class;
    }
}
