package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;

@Component
public class CDPStructuredFlowEventToCDPOperationDetailsConverter {

    @Value("${info.app.version:}")
    private String cbVersion;

    public UsageProto.CDPOperationDetails convert(CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent) {
        UsageProto.CDPOperationDetails.Builder cdpOperationDetails = UsageProto.CDPOperationDetails.newBuilder();
        CDPOperationDetails structuredOperationDetails = cdpStructuredFlowEvent.getOperation();

        cdpOperationDetails.setAccountId(structuredOperationDetails.getAccountId());
        cdpOperationDetails.setResourceCrn(structuredOperationDetails.getResourceCrn());
        cdpOperationDetails.setResourceName(structuredOperationDetails.getResourceName());
        cdpOperationDetails.setInitiatorCrn(structuredOperationDetails.getUserCrn());
        cdpOperationDetails.setApplicationVersion(cbVersion);

        return cdpOperationDetails.build();
    }

}
