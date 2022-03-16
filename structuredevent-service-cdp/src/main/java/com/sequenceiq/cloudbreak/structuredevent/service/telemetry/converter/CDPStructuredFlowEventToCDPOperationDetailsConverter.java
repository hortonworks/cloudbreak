package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.CDPRequestProcessingStepMapper;

@Component
public class CDPStructuredFlowEventToCDPOperationDetailsConverter {

    @Value("${info.app.version:}")
    private String appVersion;

    @Inject
    private CDPRequestProcessingStepMapper cdpRequestProcessingStepMapper;

    public UsageProto.CDPOperationDetails convert(CDPStructuredFlowEvent cdpStructuredFlowEvent, String cloudPlatform) {
        UsageProto.CDPOperationDetails.Builder cdpOperationDetails = UsageProto.CDPOperationDetails.newBuilder();

        if (cdpStructuredFlowEvent != null) {
            CDPOperationDetails structuredOperationDetails = cdpStructuredFlowEvent.getOperation();
            if (structuredOperationDetails != null) {
                cdpOperationDetails.setAccountId(defaultIfEmpty(structuredOperationDetails.getAccountId(), ""));
                cdpOperationDetails.setResourceCrn(defaultIfEmpty(structuredOperationDetails.getResourceCrn(), ""));
                cdpOperationDetails.setResourceName(defaultIfEmpty(structuredOperationDetails.getResourceName(), ""));
                cdpOperationDetails.setInitiatorCrn(defaultIfEmpty(structuredOperationDetails.getUserCrn(), ""));
                cdpOperationDetails.setCorrelationId(defaultIfEmpty(structuredOperationDetails.getUuid(), ""));
            }

            if (cloudPlatform != null) {
                cdpOperationDetails.setEnvironmentType(UsageProto.CDPEnvironmentsEnvironmentType.Value.valueOf(cloudPlatform));
            }

            FlowDetails flowDetails = cdpStructuredFlowEvent.getFlow();
            if (flowDetails != null) {
                String flowId = defaultIfEmpty(flowDetails.getFlowId(), "");
                cdpOperationDetails.setFlowId(flowId);
                // We will use flow id if there is no flowchain id, this helps to correlate requests
                cdpOperationDetails.setFlowChainId(defaultIfEmpty(flowDetails.getFlowChainId(), flowId));
                cdpOperationDetails.setFlowState(flowDetails.getFlowState() != null
                        && !"unknown".equals(flowDetails.getFlowState())
                        && flowDetails.getNextFlowState() != null
                        && (flowDetails.getNextFlowState().endsWith("_FAILED_STATE") || flowDetails.getNextFlowState().endsWith("_FAIL_STATE"))
                        ? flowDetails.getFlowState() : "");
            }

            cdpOperationDetails.setCdpRequestProcessingStep(cdpRequestProcessingStepMapper.mapIt(cdpStructuredFlowEvent.getFlow()));
        }
        cdpOperationDetails.setApplicationVersion(appVersion);

        return cdpOperationDetails.build();
    }
}
