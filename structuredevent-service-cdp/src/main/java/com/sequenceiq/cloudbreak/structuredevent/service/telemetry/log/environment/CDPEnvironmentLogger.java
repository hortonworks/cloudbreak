package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.environment;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.CDPEnvironmentStructuredFlowEventToCDPEnvironmentStatusChangedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.CDPTelemetryFlowEventLogger;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseMapper;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Component
public class CDPEnvironmentLogger implements CDPTelemetryFlowEventLogger<CDPEnvironmentStructuredFlowEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPEnvironmentLogger.class);

    @Inject
    private UsageReporter usageReporter;

    @Inject
    private EnvironmentUseCaseMapper environmentUseCaseMapper;

    @Inject
    private CDPEnvironmentStructuredFlowEventToCDPEnvironmentStatusChangedConverter statusChangedConverter;

    @Override
    public void log(CDPEnvironmentStructuredFlowEvent cdpStructuredEvent) {
        Value useCase = environmentUseCaseMapper.useCase(cdpStructuredEvent.getFlow());
        if (useCase != Value.UNSET) {
            LOGGER.debug("Sending usage report for {} for use case {}", cdpStructuredEvent.getOperation().getResourceType(), useCase);
            usageReporter.cdpEnvironmentStatusChanged(statusChangedConverter.convert(cdpStructuredEvent, useCase));
        }
    }

    @Override
    public Class<CDPEnvironmentStructuredFlowEvent> acceptableEventClass() {
        return CDPEnvironmentStructuredFlowEvent.class;
    }
}
