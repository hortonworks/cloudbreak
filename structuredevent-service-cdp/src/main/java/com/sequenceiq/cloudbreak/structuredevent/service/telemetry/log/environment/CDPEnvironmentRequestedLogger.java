package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.environment;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.CREATE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.CREATE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.CREATE_STARTED;

import java.util.EnumSet;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.CDPStructuredFlowEventToCDPEnvironmentRequestedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.CDPTelemetryEventLogger;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseMapper;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Component
public class CDPEnvironmentRequestedLogger implements CDPTelemetryEventLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPEnvironmentRequestedLogger.class);

    private static final EnumSet<UsageProto.CDPEnvironmentStatus.Value> TRIGGER_CASES = EnumSet.of(CREATE_STARTED, CREATE_FINISHED, CREATE_FAILED);

    @Inject
    private UsageReporter usageReporter;

    @Inject
    private EnvironmentUseCaseMapper environmentUseCaseMapper;

    @Inject
    private CDPStructuredFlowEventToCDPEnvironmentRequestedConverter converter;

    @Override
    public void log(CDPStructuredFlowEvent cdpStructuredFlowEvent) {

        FlowDetails flow = cdpStructuredFlowEvent.getFlow();
        UsageProto.CDPEnvironmentStatus.Value useCase = environmentUseCaseMapper.useCase(flow);
        LOGGER.debug("Telemetry use case: {}", useCase);

        if (cdpStructuredFlowEvent instanceof CDPEnvironmentStructuredFlowEvent && TRIGGER_CASES.contains(useCase)) {
            usageReporter.cdpEnvironmentRequested(
                    converter.convert((CDPEnvironmentStructuredFlowEvent) cdpStructuredFlowEvent));
        }
    }
}