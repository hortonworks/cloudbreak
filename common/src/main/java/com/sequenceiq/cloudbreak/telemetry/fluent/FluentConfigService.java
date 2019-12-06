package com.sequenceiq.cloudbreak.telemetry.fluent;

import java.util.HashMap;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3ConfigGenerator;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.CloudwatchParams;
import com.sequenceiq.common.api.telemetry.model.CloudwatchStreamKey;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class FluentConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FluentConfigService.class);

    private static final String S3_PROVIDER_PREFIX = "s3";

    private static final String ADLS_GEN2_PROVIDER_PREFIX = "abfs";

    private static final String CLOUDWATCH_PROVIDER_PREFIX = "cloudwatch";

    private static final String DEFAULT_PROVIDER_PREFIX = "stdout";

    private final S3ConfigGenerator s3ConfigGenerator;

    private final AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    public FluentConfigService(S3ConfigGenerator s3ConfigGenerator, AdlsGen2ConfigGenerator adlsGen2ConfigGenerator) {
        this.s3ConfigGenerator = s3ConfigGenerator;
        this.adlsGen2ConfigGenerator = adlsGen2ConfigGenerator;
    }

    public FluentConfigView createFluentConfigs(FluentClusterDetails clusterDetails,
            boolean databusEnabled, boolean meteringEnabled, Telemetry telemetry) {
        final FluentConfigView.Builder builder = new FluentConfigView.Builder();
        boolean enabled = false;
        if (telemetry != null) {
            if (telemetry.getFluentAttributes() != null) {
                builder.withOverrideAttributes(
                        telemetry.getFluentAttributes() != null ? new HashMap<>(telemetry.getFluentAttributes()) : new HashMap<>()
                );
            }
            enabled = determineAndSetLogging(builder, telemetry, databusEnabled, meteringEnabled);
        }
        if (!enabled) {
            LOGGER.debug("Fluent based logging is disabled");
        }

        return builder
                .withEnabled(enabled)
                .withClusterDetails(clusterDetails)
                .build();
    }

    private boolean determineAndSetLogging(FluentConfigView.Builder builder, Telemetry telemetry, boolean databusEnabled,
            boolean meteringEnabled) {
        boolean cloudStorageLoggingEnabled = false;
        boolean cloudLogServiceLoggingEnabled = false;
        if (telemetry.getLogging() != null) {
            Logging logging = telemetry.getLogging();
            if (logging.getS3() != null) {
                fillS3Configs(builder, logging.getStorageLocation());
                LOGGER.debug("Fluent will be configured to use S3 output.");
                cloudStorageLoggingEnabled = true;
            } else if (logging.getAdlsGen2() != null) {
                fillAdlsGen2Configs(builder, logging.getStorageLocation(), logging.getAdlsGen2());
                LOGGER.debug("Fluent will be configured to use ADLS Gen2 output.");
                cloudStorageLoggingEnabled = true;
            } else if (logging.getCloudwatch() != null) {
                fillCloudwatchConfigs(builder, logging.getStorageLocation(), logging.getCloudwatch());
                LOGGER.debug("Fluent will be configured to use Cloudwatch output.");
                cloudLogServiceLoggingEnabled = true;
            }
        }
        builder.withCloudStorageLoggingEnabled(cloudStorageLoggingEnabled)
                .withCloudLoggingServiceEnabled(cloudLogServiceLoggingEnabled);
        boolean databusLogEnabled = fillMeteringAndDeploymentReportConfigs(telemetry, databusEnabled, meteringEnabled, builder);
        return cloudStorageLoggingEnabled || databusLogEnabled || cloudLogServiceLoggingEnabled;
    }

    private boolean fillMeteringAndDeploymentReportConfigs(Telemetry telemetry, boolean databusEnabled,
            boolean meteringEnabled, FluentConfigView.Builder builder) {
        boolean validDatabusLogging = false;
        if (meteringEnabled || telemetry.isReportDeploymentLogsFeatureEnabled()) {
            if (databusEnabled && meteringEnabled) {
                builder.withMeteringEnabled(true);
                LOGGER.debug("Fluent will be configured to send metering events.");
                validDatabusLogging = true;
            }
            if (databusEnabled && telemetry.isReportDeploymentLogsFeatureEnabled()) {
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

    private void fillCloudwatchConfigs(FluentConfigView.Builder builder,
            String storageLocation, CloudwatchParams cloudwatchParams) {
        Optional<CloudwatchStreamKey> cloudwatchStreamKey = Optional.ofNullable(cloudwatchParams.getStreamKey());
        builder.withProviderPrefix(CLOUDWATCH_PROVIDER_PREFIX)
                .withRegion(cloudwatchParams.getRegion())
                .withCloudwatchStreamKey(cloudwatchStreamKey.orElse(CloudwatchStreamKey.HOSTNAME).value())
                .withLogFolderName(storageLocation);
    }
}
