package com.sequenceiq.cloudbreak.telemetry.monitoring;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.common.api.telemetry.model.Monitoring;

@Service
public class MonitoringConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringConfigService.class);

    private final MonitoringConfiguration monitoringConfiguration;

    private final MonitoringGlobalAuthConfig monitoringGlobalAuthConfig;

    public MonitoringConfigService(MonitoringConfiguration monitoringConfiguration, MonitoringGlobalAuthConfig monitoringGlobalAuthConfig) {
        this.monitoringConfiguration = monitoringConfiguration;
        this.monitoringGlobalAuthConfig = monitoringGlobalAuthConfig;
    }

    public MonitoringConfigView createMonitoringConfig(Monitoring monitoring, MonitoringClusterType clusterType,
            MonitoringAuthConfig cmAuthConfig, char[] exporterPassword, boolean cdpSaasEnabled) {
        final MonitoringConfigView.Builder builder = new MonitoringConfigView.Builder();
        boolean enabled = isMonitoringEnabled(cdpSaasEnabled);
        LOGGER.debug("Tyring to set monitoring configurations.");
        if (clusterType != null) {
            builder.withType(clusterType.value());
        }
        fillCMAuthConfigs(clusterType, cmAuthConfig, builder);
        if (enabled) {
            builder.withUseDevStack(monitoringConfiguration.isDevStack());
            if (!monitoringConfiguration.isDevStack()) {
                builder.withRemoteWriteUrl(monitoring.getRemoteWriteUrl());
            }
            builder.withScrapeIntervalSeconds(monitoringConfiguration.getScrapeIntervalSeconds());
            builder.withAgentPort(monitoringConfiguration.getAgent().getPort());
            builder.withAgentUser(monitoringConfiguration.getAgent().getUser());
            builder.withAgentMaxDiskUsage(monitoringConfiguration.getAgent().getMaxDiskUsage());
            fillExporterConfigs(builder, exporterPassword);
        }
        if (monitoringGlobalAuthConfig.isEnabled()) {
            builder.withUsername(monitoringGlobalAuthConfig.getUsername());
            if (StringUtils.isNotBlank(monitoringGlobalAuthConfig.getPassword())) {
                builder.withPassword(monitoringGlobalAuthConfig.getPassword().toCharArray());
            }
            if (StringUtils.isNotBlank(monitoringGlobalAuthConfig.getToken())) {
                builder.withToken(monitoringGlobalAuthConfig.getToken().toCharArray());
            }
        }
        return builder
                .withEnabled(enabled)
                .build();
    }

    private void fillExporterConfigs(MonitoringConfigView.Builder builder, char[] exporterPassword) {
        builder.withExporterPassword(exporterPassword);
        if (monitoringConfiguration.getNodeExporter() != null) {
            builder.withNodeExporterUser(monitoringConfiguration.getNodeExporter().getUser())
                    .withNodeExporterPort(monitoringConfiguration.getNodeExporter().getPort())
                    .withNodeExporterCollectors(monitoringConfiguration.getNodeExporter().getCollectors());
        }
        if (monitoringConfiguration.getBlackboxExporter() != null) {
                builder.withBlackboxExporterUser(monitoringConfiguration.getBlackboxExporter().getUser())
                .withBlackboxExporterPort(monitoringConfiguration.getBlackboxExporter().getPort());
        }
    }

    private void fillCMAuthConfigs(MonitoringClusterType clusterType, MonitoringAuthConfig cmAuthConfig, MonitoringConfigView.Builder builder) {
        if (MonitoringClusterType.CLOUDERA_MANAGER.equals(clusterType)) {
            LOGGER.debug("Setting up monitoring configurations for Cloudera Manager");
            if (areAuthConfigsValid(cmAuthConfig)) {
                builder
                        .withCMUsername(cmAuthConfig.getUsername())
                        .withCMPassword(cmAuthConfig.getPassword())
                        .withCMMetricsExporterPort(monitoringConfiguration.getClouderaManagerExporter().getPort());
                LOGGER.debug("Monitoring for Cloudera Manager has been setup correctly.");
            } else {
                LOGGER.debug("Monitoring for Cloudera Manager has invalid authentication configs, Monitoring will be disabled.");
            }
        }
    }

    private boolean isMonitoringEnabled(boolean cdpSaasEnabled) {
        return monitoringConfiguration.isEnabled() && (cdpSaasEnabled || monitoringConfiguration.isPaasSupport());
    }

    private boolean areAuthConfigsValid(MonitoringAuthConfig authConfig) {
        return authConfig != null && authConfig.getPassword() != null
                && StringUtils.isNoneBlank(authConfig.getUsername(), new String(authConfig.getPassword()));
    }

}
