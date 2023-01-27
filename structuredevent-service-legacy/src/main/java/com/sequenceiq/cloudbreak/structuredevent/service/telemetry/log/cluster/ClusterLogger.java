package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.StructuredFlowEventToCDPDatahubRequestedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.StructuredFlowEventToCDPDatahubStatusChangedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.StructuredFlowEventToCDPDatalakeRequestedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.StructuredFlowEventToCDPDatalakeStatusChangedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.LegacyTelemetryEventLogger;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseMapper;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Component
public class ClusterLogger implements LegacyTelemetryEventLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterLogger.class);

    @Inject
    private UsageReporter usageReporter;

    @Inject
    private ClusterUseCaseMapper clusterUseCaseMapper;

    @Inject
    private StructuredFlowEventToCDPDatalakeRequestedConverter datalakeRequestedConverter;

    @Inject
    private StructuredFlowEventToCDPDatahubRequestedConverter datahubRequestedConverter;

    @Inject
    private StructuredFlowEventToCDPDatalakeStatusChangedConverter datalakeStatusChangedConverter;

    @Inject
    private StructuredFlowEventToCDPDatahubStatusChangedConverter datahubStatusChangedConverter;

    @Override
    public void log(StructuredFlowEvent structuredFlowEvent) {
        Value useCase = clusterUseCaseMapper.useCase(structuredFlowEvent.getFlow());
        if (useCase != Value.UNSET) {
            String resourceType = structuredFlowEvent.getOperation().getResourceType();
            if (resourceType != null) {
                switch (resourceType) {
                    case "datalake":
                        LOGGER.debug("Sending usage report for {}", resourceType);
                        usageReporter.cdpDatalakeRequested(datalakeRequestedConverter.convert(structuredFlowEvent));
                        usageReporter.cdpDatalakeStatusChanged(datalakeStatusChangedConverter.convert(structuredFlowEvent, useCase));
                        break;
                    case "datahub":
                        LOGGER.debug("Sending usage report for {}", resourceType);
                        usageReporter.cdpDatahubRequested(datahubRequestedConverter.convert(structuredFlowEvent));
                        usageReporter.cdpDatahubStatusChanged(datahubStatusChangedConverter.convert(structuredFlowEvent, useCase));
                        break;
                    default:
                        LOGGER.debug("We are not sending usage report for {}", resourceType);
                }
            }
        }
    }
}