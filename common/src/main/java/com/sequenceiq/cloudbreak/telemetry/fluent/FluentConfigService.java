package com.sequenceiq.cloudbreak.telemetry.fluent;

import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3ConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.WasbConfig;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.WasbConfigGenerator;
import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class FluentConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FluentConfigService.class);

    private static final String CLUSTER_LOG_PREFIX = "cluster-logs";

    private static final String S3_PROVIDER_PREFIX = "s3";

    private static final String WASB_PROVIDER_PREFIX = "wasb";

    private static final String DEFAULT_PROVIDER_PREFIX = "stdout";

    private final S3ConfigGenerator s3ConfigGenerator;

    private final WasbConfigGenerator wasbConfigGenerator;

    public FluentConfigService(S3ConfigGenerator s3ConfigGenerator, WasbConfigGenerator wasbConfigGenerator) {
        this.s3ConfigGenerator = s3ConfigGenerator;
        this.wasbConfigGenerator = wasbConfigGenerator;
    }

    public FluentConfigView createFluentConfigs(String clusterName, String clusterType, String platform,
            boolean databusEnabled, boolean meteringEnabled, Telemetry telemetry) {
        final FluentConfigView.Builder builder = new FluentConfigView.Builder();
        boolean enabled = false;
        boolean cloudStorageLoggingEnabled = false;
        if (telemetry != null) {
            if (telemetry.getLogging() != null) {
                Logging logging = telemetry.getLogging();
                String storageLocation = logging.getStorageLocation();
                String logFolderName = Paths.get(CLUSTER_LOG_PREFIX, clusterType, clusterName).toString();

                builder.withPlatform(platform)
                        .withOverrideAttributes(
                                logging.getAttributes() != null ? new HashMap<>(logging.getAttributes()) : new HashMap<>()
                        )
                        .withProviderPrefix(DEFAULT_PROVIDER_PREFIX);

                if (logging.getS3() != null) {
                    fillS3Configs(builder, storageLocation, clusterType, clusterName, logFolderName);
                    LOGGER.debug("Fluent will be configured to use S3 output.");
                    cloudStorageLoggingEnabled = true;
                } else if (logging.getWasb() != null) {
                    fillWasbConfigs(builder, storageLocation, logging.getWasb(), clusterType, clusterName, logFolderName);
                    LOGGER.debug("Fluent will be configured to use WASB output.");
                    cloudStorageLoggingEnabled = true;
                }
                builder.withCloudStorageLoggingEnabled(cloudStorageLoggingEnabled);
            }
            boolean databusLogEnabled = fillMeteringAndDeploymentReportConfigs(telemetry, databusEnabled, meteringEnabled, builder);
            enabled = cloudStorageLoggingEnabled || databusLogEnabled;
        }
        if (!enabled) {
            LOGGER.debug("Fluent based logging is disabled");
        }

        return builder
                .withEnabled(enabled)
                .withDatabusAppName(clusterType)
                .build();
    }

    private boolean fillMeteringAndDeploymentReportConfigs(Telemetry telemetry, boolean databusEnabled,
            boolean meteringEnabled, FluentConfigView.Builder builder) {
        boolean validDatabusLogging = false;
        if (meteringEnabled || telemetry.isReportDeploymentLogs()) {
            if (databusEnabled && meteringEnabled) {
                builder.withMeteringEnabled(true);
                LOGGER.debug("Fluent will be configured to send metering events.");
                validDatabusLogging = true;
            }
            if (databusEnabled && telemetry.isReportDeploymentLogs()) {
                builder.withReportClusterDeploymentLogs(true);
                LOGGER.debug("Fluent based metering is enabled.");
                validDatabusLogging = true;
            }
        }
        return validDatabusLogging;
    }

    private void fillS3Configs(FluentConfigView.Builder builder, String storageLocation,
            String clusterType, String clusterName, String logFolderName) {
        S3Config s3Config = s3ConfigGenerator.generateStorageConfig(storageLocation);
        logFolderName = resolveLogFolder(logFolderName, s3Config.getFolderPrefix(), clusterType, clusterName);

        builder.withProviderPrefix(S3_PROVIDER_PREFIX)
                .withS3LogArchiveBucketName(s3Config.getBucket())
                .withLogFolderName(logFolderName);
    }

    // TODO: add support for Azure MSI
    private void fillWasbConfigs(FluentConfigView.Builder builder, String storageLocation,
            WasbCloudStorageV1Parameters wasbParams, String clusterType, String clusterName, String logFolderName) {
        WasbConfig wasbConfig = wasbConfigGenerator.generateStorageConfig(storageLocation);
        logFolderName = resolveLogFolder(logFolderName, wasbConfig.getFolderPrefix(), clusterType, clusterName);
        String storageAccount = StringUtils.isNotEmpty(wasbConfig.getAccount())
                ? wasbConfig.getAccount() : wasbParams.getAccountName();

        builder.withProviderPrefix(WASB_PROVIDER_PREFIX)
                .withAzureStorageAccessKey(wasbParams.getAccountKey())
                .withAzureContainer(wasbConfig.getStorageContainer())
                .withAzureStorageAccount(storageAccount)
                .withLogFolderName(logFolderName);
    }

    private String resolveLogFolder(String logFolderName, String folderPrefix, String clusterType, String clusterName) {
        if (StringUtils.isNotEmpty(folderPrefix)) {
            logFolderName = Paths.get(folderPrefix, clusterType, clusterName).toString();
        }
        return logFolderName;
    }
}
