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

    // CHECKSTYLE:OFF
    public MonitoringConfigView createMonitoringConfig(Monitoring monitoring, MonitoringClusterType clusterType,
            MonitoringAuthConfig cmAuthConfig, char[] localPassword, boolean cdpSaasEnabled, boolean computeMonitoringEnabled,
            String accessKeyId, char[] privateKey, String accessKeyType) {
        final MonitoringConfigView.Builder builder = new MonitoringConfigView.Builder();
        boolean enabled = isMonitoringEnabled(cdpSaasEnabled, computeMonitoringEnabled);
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
            builder.withRetentionMinTime(monitoringConfiguration.getAgent().getRetentionMinTime());
            builder.withRetentionMaxTime(monitoringConfiguration.getAgent().getRetentionMaxTime());
            builder.withWalTruncateFrequency(monitoringConfiguration.getAgent().getWalTruncateFrequency());
            builder.withAccessKeyId(accessKeyId);
            builder.withPrivateKey(privateKey);
            builder.withAccessKeyType(accessKeyType);
            fillExporterConfigs(builder, localPassword);
            fillRequestSignerConfigs(monitoringConfiguration.getRequestSigner(), builder);
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
    // CHECKSTYLE:ON

    private void fillExporterConfigs(MonitoringConfigView.Builder builder, char[] localPassword) {
        builder.withLocalPassword(localPassword);
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

    private void fillRequestSignerConfigs(RequestSignerConfiguration config, MonitoringConfigView.Builder builder) {
        if (config.isEnabled()) {
            RequestSignerConfigView requestSignerConfigView = RequestSignerConfigView.newBuilder()
                    .withEnabled(config.isEnabled())
                    .withPort(config.getPort())
                    .withUser(config.getUser())
                    .withUseToken(config.isUseToken())
                    .withTokenValidityMin(config.getTokenValidityMin())
                    .build();
            LOGGER.debug("Request signer is enabled, filling it for monitoring: {}", requestSignerConfigView);
            builder.withRequestSigner(requestSignerConfigView);
        }
    }

    public boolean isMonitoringEnabled(boolean cdpSaasEnabled, boolean computeMonitoringEnabled) {
        return computeMonitoringEnabled || (monitoringConfiguration.isEnabled() && (cdpSaasEnabled || monitoringConfiguration.isPaasSupport()));
    }

    private boolean areAuthConfigsValid(MonitoringAuthConfig authConfig) {
        return authConfig != null && authConfig.getPassword() != null
                && StringUtils.isNoneBlank(authConfig.getUsername(), new String(authConfig.getPassword()));
    }

}
