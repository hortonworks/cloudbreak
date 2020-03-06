package com.sequenceiq.cloudbreak.telemetry.monitoring;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;

@Service
public class MonitoringConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringConfigService.class);

    public MonitoringConfigView createMonitoringConfig(MonitoringClusterType clusterType,
            MonitoringAuthConfig authConfig, TelemetryClusterDetails clusterDetails) {
        final MonitoringConfigView.Builder builder = new MonitoringConfigView.Builder();
        boolean enabled = false;
        LOGGER.debug("Tyring to set monitoring configurations.");
        if (clusterType != null) {
            builder.withType(clusterType.value());
        }
        if (MonitoringClusterType.CLOUDERA_MANAGER.equals(clusterType)) {
            LOGGER.debug("Setting up monitoring configurations for Cloudera Manager");
            if (authConfig != null && authConfig.getPassword() != null
                    && StringUtils.isNoneBlank(authConfig.getUsername(), new String(authConfig.getPassword()))) {
                enabled = true;
                builder
                        .withCMUsername(authConfig.getUsername())
                        .withCMPassword(authConfig.getPassword());
                LOGGER.debug("Monitoring for Cloudera Manager has been setup correctly.");
            } else {
                LOGGER.debug("Monitoring for Cloudera Manager has invalid authentication configs, Monitoring will be disabled.");
            }
        }
        return builder
                .withEnabled(enabled)
                .withClusterDetails(clusterDetails)
                .build();
    }

}
