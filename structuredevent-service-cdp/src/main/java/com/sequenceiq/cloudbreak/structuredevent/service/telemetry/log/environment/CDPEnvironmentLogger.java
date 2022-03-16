package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.environment;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.CDPEnvironmentStructuredFlowEventToCDPEnvironmentRequestedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.CDPEnvironmentStructuredFlowEventToCDPEnvironmentStatusChangedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.CDPTelemetryEventLogger;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseMapper;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Component
public class CDPEnvironmentLogger implements CDPTelemetryEventLogger {

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
    public void log(CDPStructuredFlowEvent cdpStructuredFlowEvent) {

        FlowDetails flow = cdpStructuredFlowEvent.getFlow();
        UsageProto.CDPEnvironmentStatus.Value useCase = environmentUseCaseMapper.useCase(flow);
        LOGGER.debug("Telemetry use case: {}", useCase);

        if (cdpStructuredFlowEvent instanceof CDPEnvironmentStructuredFlowEvent &&
                useCase != UsageProto.CDPEnvironmentStatus.Value.UNSET) {
            LOGGER.debug("Sending usage report for {}", cdpStructuredFlowEvent.getOperation().getResourceType());
            usageReporter.cdpEnvironmentRequested(
                    environmentRequestedConverter.convert((CDPEnvironmentStructuredFlowEvent) cdpStructuredFlowEvent));
            usageReporter.cdpEnvironmentStatusChanged(
                    statusChangedConverter.convert((CDPEnvironmentStructuredFlowEvent) cdpStructuredFlowEvent, useCase));
        }
    }
}