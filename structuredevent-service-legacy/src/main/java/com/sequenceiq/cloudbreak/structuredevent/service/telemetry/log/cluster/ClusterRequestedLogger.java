package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.cluster;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.CREATE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.CREATE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.CREATE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.DOWNSCALE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.DOWNSCALE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.REPAIR_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.REPAIR_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPSCALE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPSCALE_FINISHED;

import java.util.EnumSet;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.StructuredFlowEventToCDPDatahubRequestedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.StructuredFlowEventToCDPDatalakeRequestedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.LegacyTelemetryEventLogger;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseMapper;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Component
public class ClusterRequestedLogger implements LegacyTelemetryEventLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRequestedLogger.class);

    private static final EnumSet<UsageProto.CDPClusterStatus.Value> TRIGGER_CASES = EnumSet.of(CREATE_STARTED, CREATE_FINISHED, CREATE_FAILED,
            UPGRADE_FINISHED, UPGRADE_FAILED,
            UPSCALE_FINISHED, UPSCALE_FAILED,
            DOWNSCALE_FINISHED, DOWNSCALE_FAILED,
            REPAIR_FINISHED, REPAIR_FAILED);

    @Inject
    private UsageReporter usageReporter;

    @Inject
    private ClusterUseCaseMapper clusterUseCaseMapper;

    @Inject
    private StructuredFlowEventToCDPDatalakeRequestedConverter datalakeConverter;

    @Inject
    private StructuredFlowEventToCDPDatahubRequestedConverter datahubConverter;

    @Override
    public void log(StructuredFlowEvent structuredFlowEvent) {

        FlowDetails flow = structuredFlowEvent.getFlow();
        UsageProto.CDPClusterStatus.Value useCase = clusterUseCaseMapper.useCase(flow);
        LOGGER.debug("Telemetry use case: {}", useCase);

        if (TRIGGER_CASES.contains(useCase)) {
            String resourceType = structuredFlowEvent.getOperation().getResourceType();
            if (resourceType != null) {
                switch (resourceType) {
                    case "datalake":
                        usageReporter.cdpDatalakeRequested(datalakeConverter.convert(structuredFlowEvent));
                        break;
                    case "datahub":
                        usageReporter.cdpDatahubRequested(datahubConverter.convert(structuredFlowEvent));
                        break;
                    default:
                        LOGGER.debug("We are not sending usage report for {}", resourceType);
                }
            }
        }
    }
}