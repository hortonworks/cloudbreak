package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterRequestProcessingStepMapper;

@Component
public class StructuredEventToCDPOperationDetailsConverter {

    @Value("${info.app.version:}")
    private String appVersion;

    @Inject
    private ClusterRequestProcessingStepMapper clusterRequestProcessingStepMapper;

    public UsageProto.CDPOperationDetails convert(StructuredFlowEvent structuredFlowEvent) {
        if (structuredFlowEvent == null) {
            return null;
        }
        UsageProto.CDPOperationDetails.Builder cdpOperationDetails = convert(structuredFlowEvent.getOperation(), structuredFlowEvent.getStack(),
                structuredFlowEvent.getFlow());

        cdpOperationDetails.setCdpRequestProcessingStep(clusterRequestProcessingStepMapper.mapIt(structuredFlowEvent.getFlow()));

        return cdpOperationDetails.build();
    }

    public UsageProto.CDPOperationDetails convert(StructuredSyncEvent structuredSyncEvent) {
        if (structuredSyncEvent == null) {
            return null;
        }
        UsageProto.CDPOperationDetails.Builder cdpOperationDetails = convert(structuredSyncEvent.getOperation(), structuredSyncEvent.getStack(), null);

        cdpOperationDetails.setCdpRequestProcessingStep(UsageProto.CDPRequestProcessingStep.Value.SYNC);

        return cdpOperationDetails.build();
    }

    private UsageProto.CDPOperationDetails.Builder convert(OperationDetails structuredOperationDetails, StackDetails stackDetails, FlowDetails flowDetails) {

        UsageProto.CDPOperationDetails.Builder cdpOperationDetails = UsageProto.CDPOperationDetails.newBuilder();

        if (structuredOperationDetails != null) {
            cdpOperationDetails.setAccountId(defaultIfEmpty(structuredOperationDetails.getTenant(), ""));
            cdpOperationDetails.setResourceCrn(defaultIfEmpty(structuredOperationDetails.getResourceCrn(), ""));
            cdpOperationDetails.setResourceName(defaultIfEmpty(structuredOperationDetails.getResourceName(), ""));
            cdpOperationDetails.setInitiatorCrn(defaultIfEmpty(structuredOperationDetails.getUserCrn(), ""));
            cdpOperationDetails.setCorrelationId(defaultIfEmpty(structuredOperationDetails.getUuid(), ""));
        }

        if (stackDetails != null && stackDetails.getCloudPlatform() != null) {
            cdpOperationDetails.setEnvironmentType(UsageProto.CDPEnvironmentsEnvironmentType
                    .Value.valueOf(stackDetails.getCloudPlatform()));
        }

        if (flowDetails != null) {
            String flowId = defaultIfEmpty(flowDetails.getFlowId(), "");
            cdpOperationDetails.setFlowId(flowId);
            // We will use flow id if there is no flowchain id, this helps to correlate requests
            cdpOperationDetails.setFlowChainId(defaultIfEmpty(flowDetails.getFlowChainId(), flowId));
            cdpOperationDetails.setFlowState(flowDetails.getFlowState() != null &&
                    !"unknown".equals(flowDetails.getFlowState()) ? flowDetails.getFlowState() : "");
        }

        cdpOperationDetails.setApplicationVersion(appVersion);

        return cdpOperationDetails;
    }
}
