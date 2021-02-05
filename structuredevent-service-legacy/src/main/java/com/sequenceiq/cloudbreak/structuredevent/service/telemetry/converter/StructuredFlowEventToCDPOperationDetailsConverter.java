package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterRequestProcessingStepMapper;

@Component
public class StructuredFlowEventToCDPOperationDetailsConverter {

    @Value("${info.app.version:}")
    private String appVersion;

    @Inject
    private ClusterRequestProcessingStepMapper clusterRequestProcessingStepMapper;

    public UsageProto.CDPOperationDetails convert(StructuredFlowEvent structuredFlowEvent) {
        if (structuredFlowEvent == null) {
            return null;
        }
        UsageProto.CDPOperationDetails.Builder cdpOperationDetails = UsageProto.CDPOperationDetails.newBuilder();
        OperationDetails structuredOperationDetails = structuredFlowEvent.getOperation();
        if (structuredOperationDetails != null) {
            cdpOperationDetails.setAccountId(structuredOperationDetails.getTenant());
            cdpOperationDetails.setResourceCrn(structuredOperationDetails.getResourceCrn());
            cdpOperationDetails.setResourceName(structuredOperationDetails.getResourceName());
            cdpOperationDetails.setInitiatorCrn(structuredOperationDetails.getUserCrn());
        }

        FlowDetails flowDetails = structuredFlowEvent.getFlow();
        if (flowDetails != null) {
            String flowId = flowDetails.getFlowId() != null ? flowDetails.getFlowId() : "";
            cdpOperationDetails.setFlowId(flowId);
            // We will use flow id if there is no flowchain id, this helps to correlate requests
            cdpOperationDetails.setFlowChainId(flowDetails.getFlowChainId() != null ? flowDetails.getFlowChainId() : flowId);
            cdpOperationDetails.setFlowState(flowDetails.getFlowState() != null &&
                    !"unknown".equals(flowDetails.getFlowState()) ? flowDetails.getFlowState() : "");
        }

        cdpOperationDetails.setCdpRequestProcessingStep(clusterRequestProcessingStepMapper.mapIt(structuredFlowEvent.getFlow()));
        cdpOperationDetails.setApplicationVersion(appVersion);

        return cdpOperationDetails.build();
    }
}
