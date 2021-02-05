package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.model.CombinedStatus;

@Component
public class StructuredFlowEventToCDPDatalakeStatusChangedConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredFlowEventToCDPDatalakeStatusChangedConverter.class);

    @Inject
    private StructuredFlowEventToCDPOperationDetailsConverter operationDetailsConverter;

    @Inject
    private StructuredFlowEventToCombinedStatusConverter combinedStatusConverter;

    public UsageProto.CDPDatalakeStatusChanged convert(StructuredFlowEvent structuredFlowEvent, UsageProto.CDPClusterStatus.Value status) {
        if (structuredFlowEvent == null) {
            return null;
        }
        UsageProto.CDPDatalakeStatusChanged.Builder cdpDatalakeStatusChanged = UsageProto.CDPDatalakeStatusChanged.newBuilder();
        cdpDatalakeStatusChanged.setOperationDetails(operationDetailsConverter.convert(structuredFlowEvent));

        cdpDatalakeStatusChanged.setNewStatus(status);

        CombinedStatus combinedStatus = combinedStatusConverter.convert(structuredFlowEvent);
        cdpDatalakeStatusChanged.setFailureReason(JsonUtil.writeValueAsStringSilentSafe(combinedStatus));

        UsageProto.CDPDatalakeStatusChanged ret = cdpDatalakeStatusChanged.build();
        LOGGER.debug("Converted CDPDatalakeStatusChanged telemetry event: {}", ret);
        return ret;
    }
}
