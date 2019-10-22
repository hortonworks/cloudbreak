package com.sequenceiq.cloudbreak.telemetry.fluent;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3ConfigGenerator;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class FluentConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FluentConfigService.class);

    private static final String S3_PROVIDER_PREFIX = "s3";

    private static final String ADLS_GEN2_PROVIDER_PREFIX = "abfs";

    private static final String DEFAULT_PROVIDER_PREFIX = "stdout";

    private final S3ConfigGenerator s3ConfigGenerator;

    private final AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    public FluentConfigService(S3ConfigGenerator s3ConfigGenerator, AdlsGen2ConfigGenerator adlsGen2ConfigGenerator) {
        this.s3ConfigGenerator = s3ConfigGenerator;
        this.adlsGen2ConfigGenerator = adlsGen2ConfigGenerator;
    }

    public FluentConfigView createFluentConfigs(String clusterType, String platform,
            boolean databusEnabled, boolean meteringEnabled, Telemetry telemetry) {
        final FluentConfigView.Builder builder = new FluentConfigView.Builder();
        boolean enabled = false;
        boolean cloudStorageLoggingEnabled = false;
        if (telemetry != null) {
            if (telemetry.getLogging() != null) {
                Logging logging = telemetry.getLogging();
                builder.withPlatform(platform)
                        .withOverrideAttributes(
                                logging.getAttributes() != null ? new HashMap<>(logging.getAttributes()) : new HashMap<>()
                        )
                        .withProviderPrefix(DEFAULT_PROVIDER_PREFIX);

                if (logging.getS3() != null) {
                    fillS3Configs(builder, logging.getStorageLocation());
                    LOGGER.debug("Fluent will be configured to use S3 output.");
                    cloudStorageLoggingEnabled = true;
                } else if (logging.getAdlsGen2() != null) {
                    fillAdlsGen2Configs(builder, logging.getStorageLocation(), logging.getAdlsGen2());
                    LOGGER.debug("Fluent will be configured to use ADLS Gen2 output.");
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

    private void fillS3Configs(FluentConfigView.Builder builder, String storageLocation) {
        S3Config s3Config = s3ConfigGenerator.generateStorageConfig(storageLocation);

        builder.withProviderPrefix(S3_PROVIDER_PREFIX)
                .withS3LogArchiveBucketName(s3Config.getBucket())
                .withLogFolderName(s3Config.getFolderPrefix());
    }

    private void fillAdlsGen2Configs(FluentConfigView.Builder builder, String storageLocation,
            AdlsGen2CloudStorageV1Parameters parameters) {
        AdlsGen2Config adlsGen2Config = adlsGen2ConfigGenerator.generateStorageConfig(storageLocation);
        String storageAccount = StringUtils.isNotEmpty(adlsGen2Config.getAccount())
                ? adlsGen2Config.getAccount() : parameters.getAccountName();

        if (StringUtils.isNotBlank(parameters.getManagedIdentity())) {
            builder.withAzureInstanceMsi(parameters.getManagedIdentity());
        } else {
            builder.withAzureStorageAccessKey(parameters.getAccountKey());
        }

        builder.withProviderPrefix(ADLS_GEN2_PROVIDER_PREFIX)
                .withAzureContainer(adlsGen2Config.getFileSystem())
                .withAzureStorageAccount(storageAccount)
                .withLogFolderName(adlsGen2Config.getFolderPrefix());
    }
}
