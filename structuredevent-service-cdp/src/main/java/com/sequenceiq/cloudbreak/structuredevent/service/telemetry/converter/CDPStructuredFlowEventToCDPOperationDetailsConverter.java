package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentRequestProcessingStepMapper;

@Component
public class CDPStructuredFlowEventToCDPOperationDetailsConverter {

    @Value("${info.app.version:}")
    private String appVersion;

    @Inject
    private EnvironmentRequestProcessingStepMapper environmentRequestProcessingStepMapper;

    public UsageProto.CDPOperationDetails convert(CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent) {
        if (cdpStructuredFlowEvent == null) {
            return null;
        }
        UsageProto.CDPOperationDetails.Builder cdpOperationDetails = UsageProto.CDPOperationDetails.newBuilder();
        CDPOperationDetails structuredOperationDetails = cdpStructuredFlowEvent.getOperation();
        if (structuredOperationDetails != null) {
            cdpOperationDetails.setAccountId(structuredOperationDetails.getAccountId());
            cdpOperationDetails.setResourceCrn(structuredOperationDetails.getResourceCrn());
            cdpOperationDetails.setResourceName(structuredOperationDetails.getResourceName());
            cdpOperationDetails.setInitiatorCrn(structuredOperationDetails.getUserCrn());
        }

        FlowDetails flowDetails = cdpStructuredFlowEvent.getFlow();
        if (flowDetails != null) {
            String flowId = flowDetails.getFlowId() != null ? flowDetails.getFlowId() : "";
            cdpOperationDetails.setFlowId(flowId);
            // We will use flow id if there is no flowchain id, this helps to correlate requests
            cdpOperationDetails.setFlowChainId(flowDetails.getFlowChainId() != null ? flowDetails.getFlowChainId() : flowId);
            cdpOperationDetails.setFlowState(flowDetails.getFlowState() != null &&
                    !"unknown".equals(flowDetails.getFlowState()) ? flowDetails.getFlowState() : "");
        }

        cdpOperationDetails.setCdpRequestProcessingStep(environmentRequestProcessingStepMapper.mapIt(cdpStructuredFlowEvent.getFlow()));
        cdpOperationDetails.setApplicationVersion(appVersion);

        return cdpOperationDetails.build();
    }
}
