package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

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
    private StructuredEventToCDPOperationDetailsConverter operationDetailsConverter;

    @Inject
    private StructuredEventToClusterDetailsConverter clusterDetailsConverter;

    public UsageProto.CDPDatahubRequested convert(StructuredFlowEvent structuredFlowEvent) {
        UsageProto.CDPDatahubRequested.Builder cdpDatahubRequested = UsageProto.CDPDatahubRequested.newBuilder();

        cdpDatahubRequested.setOperationDetails(operationDetailsConverter.convert(structuredFlowEvent));

        if (structuredFlowEvent != null && structuredFlowEvent.getOperation() != null) {
            cdpDatahubRequested.setEnvironmentCrn(defaultIfEmpty(structuredFlowEvent.getOperation().getEnvironmentCrn(), ""));
        }

        cdpDatahubRequested.setClusterDetails(clusterDetailsConverter.convert(structuredFlowEvent));

        UsageProto.CDPDatahubRequested ret = cdpDatahubRequested.build();
        LOGGER.debug("Converted CDPDatahubRequested telemetry event: {}", ret);
        return ret;
    }
}
