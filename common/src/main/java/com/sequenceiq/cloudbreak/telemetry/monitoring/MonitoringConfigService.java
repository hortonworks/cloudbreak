package com.sequenceiq.cloudbreak.telemetry.monitoring;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.telemetry.TelemetryPillarConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.UMSSecretKeyFormatter;
import com.sequenceiq.cloudbreak.telemetry.context.MonitoringContext;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;

@Service
public class MonitoringConfigService implements TelemetryPillarConfigGenerator<MonitoringConfigView> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringConfigService.class);

    private static final String DEFAULT_ACCESS_KEY_TYPE = "Ed25519";

    private static final String SALT_STATE = "monitoring";

    private final MonitoringConfiguration monitoringConfiguration;

    private final MonitoringGlobalAuthConfig monitoringGlobalAuthConfig;

    public MonitoringConfigService(MonitoringConfiguration monitoringConfiguration, MonitoringGlobalAuthConfig monitoringGlobalAuthConfig) {
        this.monitoringConfiguration = monitoringConfiguration;
        this.monitoringGlobalAuthConfig = monitoringGlobalAuthConfig;
    }

    @Override
    public MonitoringConfigView createConfigs(TelemetryContext context) {
        final MonitoringContext monitoringContext = context.getMonitoringContext();
        final MonitoringConfigView.Builder builder = new MonitoringConfigView.Builder();
        LOGGER.debug("Tyring to set monitoring configurations.");
        if (monitoringContext.getClusterType() != null) {
            builder.withType(monitoringContext.getClusterType().value());
        }
        fillCMAuthConfigs(monitoringContext.getClusterType(), monitoringContext.getCmAuth(), builder);
        builder.withCmAutoTls(monitoringContext.isCmAutoTls());
        builder.withRemoteWriteUrl(monitoringContext.getRemoteWriteUrl());
        builder.withScrapeIntervalSeconds(monitoringConfiguration.getScrapeIntervalSeconds());
        builder.withAgentPort(monitoringConfiguration.getAgent().getPort());
        builder.withAgentUser(monitoringConfiguration.getAgent().getUser());
        builder.withAgentMaxDiskUsage(monitoringConfiguration.getAgent().getMaxDiskUsage());
        builder.withRetentionMinTime(monitoringConfiguration.getAgent().getRetentionMinTime());
        builder.withRetentionMaxTime(monitoringConfiguration.getAgent().getRetentionMaxTime());
        builder.withWalTruncateFrequency(monitoringConfiguration.getAgent().getWalTruncateFrequency());
        builder.withMinBackoff(monitoringConfiguration.getAgent().getMinBackoff());
        builder.withMaxBackoff(monitoringConfiguration.getAgent().getMaxBackoff());
        builder.withMaxShards(monitoringConfiguration.getAgent().getMaxShards());
        builder.withMaxSamplesPerSend(monitoringConfiguration.getAgent().getMaxSamplesPerSend());
        builder.withCapacity(monitoringConfiguration.getAgent().getCapacity());
        if (monitoringContext.getCredential() != null) {
            String accessKeyType = StringUtils.defaultIfBlank(monitoringContext.getCredential().getAccessKeyType(), DEFAULT_ACCESS_KEY_TYPE);
            builder.withAccessKeyId(monitoringContext.getCredential().getAccessKey())
                    .withPrivateKey(UMSSecretKeyFormatter.formatSecretKey(accessKeyType, monitoringContext.getCredential().getPrivateKey()).toCharArray())
                    .withAccessKeyType(accessKeyType);
        }
        fillExporterConfigs(builder, monitoringContext.getSharedPassword());
        fillRequestSignerConfigs(monitoringConfiguration.getRequestSigner(), builder);
        if (monitoringGlobalAuthConfig.isEnabled()) {
            builder.withUsername(monitoringGlobalAuthConfig.getUsername());
            if (StringUtils.isNotBlank(monitoringGlobalAuthConfig.getPassword())) {
                builder.withPassword(monitoringGlobalAuthConfig.getPassword());
            }
            if (StringUtils.isNotBlank(monitoringGlobalAuthConfig.getToken())) {
                builder.withToken(monitoringGlobalAuthConfig.getToken().toCharArray());
            }
        }
        return builder
                .withEnabled(monitoringContext.isEnabled())
                .build();
    }

    @Override
    public boolean isEnabled(TelemetryContext context) {
        return context != null && context.getMonitoringContext() != null && context.getMonitoringContext().isEnabled();
    }

    @Override
    public String saltStateName() {
        return SALT_STATE;
    }

    private void fillExporterConfigs(MonitoringConfigView.Builder builder, String localPassword) {
        builder.withLocalPassword(localPassword);
        if (monitoringConfiguration.getNodeExporter() != null) {
            builder.withNodeExporterUser(monitoringConfiguration.getNodeExporter().getUser())
                    .withNodeExporterPort(monitoringConfiguration.getNodeExporter().getPort())
                    .withNodeExporterCollectors(monitoringConfiguration.getNodeExporter().getCollectors());
        }
        if (monitoringConfiguration.getBlackboxExporter() != null) {
            builder.withBlackboxExporterUser(monitoringConfiguration.getBlackboxExporter().getUser())
                    .withBlackboxExporterPort(monitoringConfiguration.getBlackboxExporter().getPort())
                    .withBlackboxExporterCheckOnAllNodes(monitoringConfiguration.getBlackboxExporter().isCheckOnAllNodes())
                    .withBlackboxExporterClouderaIntervalSeconds(monitoringConfiguration.getBlackboxExporter().getClouderaIntervalSeconds())
                    .withBlackboxExporterCloudIntervalSeconds(monitoringConfiguration.getBlackboxExporter().getCloudIntervalSeconds());
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

    private boolean areAuthConfigsValid(MonitoringAuthConfig authConfig) {
        return authConfig != null && authConfig.getPassword() != null
                && StringUtils.isNoneBlank(authConfig.getUsername(), authConfig.getPassword());
    }
}
