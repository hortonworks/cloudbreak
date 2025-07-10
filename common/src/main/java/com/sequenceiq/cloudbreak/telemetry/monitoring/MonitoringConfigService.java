package com.sequenceiq.cloudbreak.telemetry.monitoring;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.fromName;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.telemetry.TelemetryPillarConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.UMSSecretKeyFormatter;
import com.sequenceiq.cloudbreak.telemetry.context.DatabusContext;
import com.sequenceiq.cloudbreak.telemetry.context.MonitoringContext;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfigView.Builder;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;

@Service
public class MonitoringConfigService implements TelemetryPillarConfigGenerator<MonitoringConfigView> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringConfigService.class);

    private static final String DEFAULT_ACCESS_KEY_TYPE = "Ed25519";

    private static final String SALT_STATE = "monitoring";

    private static final String AWS_SERVICE_DOMAIN = "amazonaws.com";

    private final MonitoringConfiguration monitoringConfiguration;

    private final AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    public MonitoringConfigService(MonitoringConfiguration monitoringConfiguration, AdlsGen2ConfigGenerator adlsGen2ConfigGenerator) {
        this.monitoringConfiguration = monitoringConfiguration;
        this.adlsGen2ConfigGenerator = adlsGen2ConfigGenerator;
    }

    @Override
    public MonitoringConfigView createConfigs(TelemetryContext context) {
        final MonitoringContext monitoringContext = context.getMonitoringContext();
        final Builder builder = new Builder();
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
        builder.withTlsCipherSuit(context.getTlsCipherSuites());

        if (monitoringContext.getCredential() != null) {
            String accessKeyType = StringUtils.defaultIfBlank(monitoringContext.getCredential().getAccessKeyType(), DEFAULT_ACCESS_KEY_TYPE);
            builder.withAccessKeyId(monitoringContext.getCredential().getAccessKey())
                    .withPrivateKey(UMSSecretKeyFormatter.formatSecretKey(accessKeyType, monitoringContext.getCredential().getPrivateKey()).toCharArray())
                    .withAccessKeyType(accessKeyType);
        }
        fillExporterConfigs(builder, monitoringContext.getSharedPassword(), context);
        fillRequestSignerConfigs(monitoringConfiguration.getRequestSigner(), builder);
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

    private void fillExporterConfigs(Builder builder, String localPassword, TelemetryContext telemetryContext) {
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
                    .withBlackboxExporterCloudIntervalSeconds(monitoringConfiguration.getBlackboxExporter().getCloudIntervalSeconds())
                    .withBlackboxExporterCloudLinks(generateCloudLinks(fromName(telemetryContext.getCloudPlatform()),
                            telemetryContext.getRegion(), telemetryContext.getTelemetry().getLogging()))
                    .withBlackboxExporterClouderaLinks(generateClouderaLinks(telemetryContext.getDatabusContext()));
        }
    }

    private List<String> generateCloudLinks(CloudPlatform platform, String region, Logging telemetryLogging) {
        List<String> result = new ArrayList<>();
        if (platform != null) {
            switch (platform) {
                case AWS:
                    result.add(formatServiceLink("s3", region));
                    result.add(formatServiceLink("sls", region));
                    break;
                case AZURE:
                    result.add("https://management.azure.com");
                    extractStorageAccount(telemetryLogging)
                            .ifPresent(storageAccount -> result.add(String.format("https://%s.dfs.core.windows.net", storageAccount)));
                    break;
                case GCP:
                    result.add("https://storage.googleapis.com");
                    break;
                default:
            }
        }
        return result;
    }

    private Optional<String> extractStorageAccount(Logging logging) {
        Optional<String> storageAccountOptional = Optional.empty();
        if (logging != null && logging.getAdlsGen2() != null) {
            String storageLocation = logging.getStorageLocation();
            AdlsGen2CloudStorageV1Parameters parameters = logging.getAdlsGen2();
            AdlsGen2Config adlsGen2Config = adlsGen2ConfigGenerator.generateStorageConfig(storageLocation);
            String storageAccount = StringUtils.isNotEmpty(adlsGen2Config.getAccount())
                    ? adlsGen2Config.getAccount() : parameters.getAccountName();
            storageAccountOptional = Optional.ofNullable(storageAccount);
        }
        return storageAccountOptional;
    }

    private String formatServiceLink(String service, String region) {
        String serviceName = service;
        if (region != null && region.contains("-gov-")) {
            serviceName += "-fips";
        }
        return String.format("https://%s.%s.%s", serviceName, region, AWS_SERVICE_DOMAIN);
    }

    private List<String> generateClouderaLinks(DatabusContext databusContext) {
        List<String> result = new ArrayList<>();
        result.add("https://archive.cloudera.com");
        result.add("https://cloudera-service-delivery-cache.s3.amazonaws.com");
        if (StringUtils.isNotBlank(databusContext.getEndpoint())) {
            result.add(databusContext.getEndpoint());
        }
        if (StringUtils.isNotBlank(databusContext.getS3Endpoint())) {
            result.add(databusContext.getS3Endpoint());
        }
        return result;
    }

    private void fillCMAuthConfigs(MonitoringClusterType clusterType, MonitoringAuthConfig cmAuthConfig, Builder builder) {
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

    private void fillRequestSignerConfigs(RequestSignerConfiguration config, Builder builder) {
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
