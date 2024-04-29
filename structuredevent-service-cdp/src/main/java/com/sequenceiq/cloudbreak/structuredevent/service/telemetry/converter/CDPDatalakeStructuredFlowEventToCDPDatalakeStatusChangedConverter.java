package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPDatalakeStatusChanged;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.datalake.CDPDatalakeStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.datalake.DatalakeDetails;

@Component
public class CDPDatalakeStructuredFlowEventToCDPDatalakeStatusChangedConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPDatalakeStructuredFlowEventToCDPDatalakeStatusChangedConverter.class);

    @Inject
    private CDPStructuredFlowEventToCDPOperationDetailsConverter operationDetailsConverter;

    @Inject
    private DatalakeDetailsToCDPStatusDetailsConverter statusDetailsConverter;

    @Inject
    private DatalakeDetailsToCDPDatalakeFeaturesConverter featuresConverter;

    @Inject
    private DatalakeDetailsToCDPClusterDetailsConverter clusterDetailsConverter;

    public CDPDatalakeStatusChanged convert(CDPDatalakeStructuredFlowEvent cdpStructuredFlowEvent, CDPClusterStatus.Value status) {
        CDPDatalakeStatusChanged.Builder cdpDatalakeStatusChanged = CDPDatalakeStatusChanged.newBuilder();

        if (cdpStructuredFlowEvent != null) {
            DatalakeDetails datalakeDetails = cdpStructuredFlowEvent.getPayload();
            String cloudProvider = datalakeDetails != null ? datalakeDetails.getCloudPlatform() : null;
            cdpDatalakeStatusChanged.setOperationDetails(operationDetailsConverter.convert(cdpStructuredFlowEvent, cloudProvider));
            if (datalakeDetails != null) {
                cdpDatalakeStatusChanged.setStatusDetails(statusDetailsConverter.convert(datalakeDetails));
                cdpDatalakeStatusChanged.setFeatures(featuresConverter.convert(datalakeDetails));
                cdpDatalakeStatusChanged.setClusterDetails(clusterDetailsConverter.convert(datalakeDetails));
            }
            if (cdpStructuredFlowEvent.getOperation() != null) {
                cdpDatalakeStatusChanged.setEnvironmentCrn(cdpStructuredFlowEvent.getOperation().getEnvironmentCrn());
            }
        }
        cdpDatalakeStatusChanged.setNewStatus(status);

        CDPDatalakeStatusChanged ret = cdpDatalakeStatusChanged.build();
        LOGGER.debug("Converted CDPDatalakeStatusChanged telemetry event: {}", ret);
        return ret;
    }
}
