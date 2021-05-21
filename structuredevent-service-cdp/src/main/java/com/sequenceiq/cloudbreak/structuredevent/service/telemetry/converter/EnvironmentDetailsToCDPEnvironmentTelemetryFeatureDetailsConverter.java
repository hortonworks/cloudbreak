package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;

@Component
public class EnvironmentDetailsToCDPEnvironmentTelemetryFeatureDetailsConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDetailsToCDPEnvironmentTelemetryFeatureDetailsConverter.class);

    public UsageProto.CDPEnvironmentTelemetryFeatureDetails convert(EnvironmentDetails environmentDetails) {
        UsageProto.CDPEnvironmentTelemetryFeatureDetails.Builder cdpTelemetryFeatureDetailsBuilder =
                UsageProto.CDPEnvironmentTelemetryFeatureDetails.newBuilder();

        if (environmentDetails != null && environmentDetails.getEnvironmentTelemetryFeatures() != null) {
            EnvironmentFeatures environmentFeatures = environmentDetails.getEnvironmentTelemetryFeatures();
            if (environmentFeatures.getWorkloadAnalytics() != null && environmentFeatures.getWorkloadAnalytics().isEnabled() != null) {
                cdpTelemetryFeatureDetailsBuilder.setWorkloadAnalytics(environmentFeatures.getWorkloadAnalytics().isEnabled().toString());
            }
            if (environmentFeatures.getClusterLogsCollection() != null && environmentFeatures.getClusterLogsCollection().isEnabled() != null) {
                cdpTelemetryFeatureDetailsBuilder.setClusterLogsCollection(environmentFeatures.getClusterLogsCollection().isEnabled().toString());
            }
        }

        UsageProto.CDPEnvironmentTelemetryFeatureDetails ret = cdpTelemetryFeatureDetailsBuilder.build();
        LOGGER.debug("Converted CDPEnvironmentTelemetryFeatureDetails: {}", ret);
        return ret;
    }
}
