package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class StructuredFlowEventToCDPDatahubRequestedConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredFlowEventToCDPDatahubRequestedConverter.class);

    @Inject
    private StructuredFlowEventToCDPOperationDetailsConverter operationDetailsConverter;

    @Inject
    private StructuredFlowEventToClusterDetailsConverter clusterDetailsConverter;

    public UsageProto.CDPDatahubRequested convert(StructuredFlowEvent structuredFlowEvent) {
        if (structuredFlowEvent == null) {
            return null;
        }
        UsageProto.CDPDatahubRequested.Builder cdpDatahubRequested = UsageProto.CDPDatahubRequested.newBuilder();
        cdpDatahubRequested.setOperationDetails(operationDetailsConverter.convert(structuredFlowEvent));

        cdpDatahubRequested.setEnvironmentCrn(structuredFlowEvent.getOperation().getEnvironmentCrn());

        cdpDatahubRequested.setClusterDetails(clusterDetailsConverter.convert(structuredFlowEvent));

        UsageProto.CDPDatahubRequested ret = cdpDatahubRequested.build();
        LOGGER.debug("Converted CDPDatahubRequested telemetry event: {}", ret);
        return ret;
    }
}
