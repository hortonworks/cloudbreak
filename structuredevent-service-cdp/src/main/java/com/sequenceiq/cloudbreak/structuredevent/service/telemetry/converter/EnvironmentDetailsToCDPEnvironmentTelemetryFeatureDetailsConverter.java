package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentTelemetryFeatureDetails;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;

@Component
public class EnvironmentDetailsToCDPEnvironmentTelemetryFeatureDetailsConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDetailsToCDPEnvironmentTelemetryFeatureDetailsConverter.class);

    public CDPEnvironmentTelemetryFeatureDetails convert(EnvironmentDetails environmentDetails) {
        CDPEnvironmentTelemetryFeatureDetails.Builder cdpTelemetryFeatureDetailsBuilder = CDPEnvironmentTelemetryFeatureDetails.newBuilder();

        if (environmentDetails != null) {
            if (environmentDetails.getEnvironmentTelemetryFeatures() != null) {
                EnvironmentFeatures environmentFeatures = environmentDetails.getEnvironmentTelemetryFeatures();
                if (environmentFeatures.getWorkloadAnalytics() != null && environmentFeatures.getWorkloadAnalytics().getEnabled() != null) {
                    cdpTelemetryFeatureDetailsBuilder.setWorkloadAnalytics(environmentFeatures.getWorkloadAnalytics().getEnabled().toString());
                }
            }
            if (environmentDetails.getTelemetryDetails() != null) {
                cdpTelemetryFeatureDetailsBuilder.setStorageLocationBase(
                        defaultIfNull(environmentDetails.getTelemetryDetails().storageLocationBase(), ""));
                cdpTelemetryFeatureDetailsBuilder.setBackupStorageLocationBase(
                        defaultIfNull(environmentDetails.getTelemetryDetails().backupStorageLocationBase(), ""));
            }
        }

        CDPEnvironmentTelemetryFeatureDetails ret = cdpTelemetryFeatureDetailsBuilder.build();
        LOGGER.debug("Converted CDPEnvironmentTelemetryFeatureDetails: {}", ret);
        return ret;
    }
}
