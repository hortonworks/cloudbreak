package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;

@Component
public class CDPStructuredFlowEventToCDPEnvironmentStatusChangedConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPStructuredFlowEventToCDPEnvironmentStatusChangedConverter.class);

    @Inject
    private CDPStructuredFlowEventToCDPOperationDetailsConverter operationDetailsConverter;

    @Inject
    private EnvironmentDetailsToCDPEnvironmentDetailsConverter environmentDetailsConverter;

    @Inject
    private EnvironmentDetailsToCDPFreeIPADetailsConverter freeIPADetailsConverter;

    @Inject
    private EnvironmentDetailsToCDPEnvironmentTelemetryFeatureDetailsConverter telemetryFeatureDetailsConverter;

    public UsageProto.CDPEnvironmentStatusChanged convert(CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent,
            UsageProto.CDPEnvironmentStatus.Value status) {
        UsageProto.CDPEnvironmentStatusChanged.Builder cdpEnvironmentStatusChangedBuilder = UsageProto.CDPEnvironmentStatusChanged.newBuilder();

        cdpEnvironmentStatusChangedBuilder.setOperationDetails(operationDetailsConverter.convert(cdpStructuredFlowEvent));

        cdpEnvironmentStatusChangedBuilder.setNewStatus(status);

        if (cdpStructuredFlowEvent != null) {
            cdpEnvironmentStatusChangedBuilder.setFailureReason(defaultIfEmpty(cdpStructuredFlowEvent.getStatusReason(), ""));

            EnvironmentDetails environmentDetails = cdpStructuredFlowEvent.getPayload();
            cdpEnvironmentStatusChangedBuilder.setEnvironmentDetails(environmentDetailsConverter.convert(environmentDetails));
            cdpEnvironmentStatusChangedBuilder.setTelemetryFeatureDetails(telemetryFeatureDetailsConverter.convert(environmentDetails));
            cdpEnvironmentStatusChangedBuilder.setFreeIPA(freeIPADetailsConverter.convert(environmentDetails));
        }

        UsageProto.CDPEnvironmentStatusChanged ret = cdpEnvironmentStatusChangedBuilder.build();
        LOGGER.debug("Converted CDPEnvironmentStatusChanged event: {}", ret);
        return ret;
    }
}
