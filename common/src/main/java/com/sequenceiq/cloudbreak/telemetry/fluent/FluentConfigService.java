package com.sequenceiq.cloudbreak.telemetry.fluent;

import java.util.HashMap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfiguration;
import com.sequenceiq.cloudbreak.telemetry.TelemetryPillarConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.common.AnonymizationRuleResolver;
import com.sequenceiq.cloudbreak.telemetry.context.LogShipperContext;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.GcsConfig;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.GcsConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3ConfigGenerator;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.SensitiveLoggingComponent;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class FluentConfigService implements TelemetryPillarConfigGenerator<FluentConfigView> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FluentConfigService.class);

    private static final String SALT_STATE = "fluent";

    private static final String S3_PROVIDER_PREFIX = "s3";

    private static final String ADLS_GEN2_PROVIDER_PREFIX = "abfs";

    private static final String GCS_PROVIDER_PREFIX = "gcs";

    private static final String CLOUDWATCH_PROVIDER_PREFIX = "cloudwatch";

    private final S3ConfigGenerator s3ConfigGenerator;

    private final AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    private final GcsConfigGenerator gcsConfigGenerator;

    private final AnonymizationRuleResolver anonymizationRuleResolver;

    public FluentConfigService(S3ConfigGenerator s3ConfigGenerator,
            AdlsGen2ConfigGenerator adlsGen2ConfigGenerator,
            GcsConfigGenerator gcsConfigGenerator,
            AnonymizationRuleResolver anonymizationRuleResolver,
            TelemetryConfiguration telemetryConfiguration) {
        this.s3ConfigGenerator = s3ConfigGenerator;
        this.adlsGen2ConfigGenerator = adlsGen2ConfigGenerator;
        this.gcsConfigGenerator = gcsConfigGenerator;
        this.anonymizationRuleResolver = anonymizationRuleResolver;
    }

    @Override
    public FluentConfigView createConfigs(TelemetryContext context) {
        final LogShipperContext logShipperContext = context.getLogShipperContext();
        final Telemetry telemetry = context.getTelemetry();
        final FluentConfigView.Builder builder = new FluentConfigView.Builder();
        if (telemetry.getFluentAttributes() != null) {
            builder.withOverrideAttributes(
                    telemetry.getFluentAttributes() != null ? new HashMap<>(telemetry.getFluentAttributes()) : new HashMap<>()
            );
        }
        if (logShipperContext.isCloudStorageLogging()) {
            builder.withEnabled(true);
            Logging logging = telemetry.getLogging();
            setupCloudStorageLogging(builder, logging);
            if (CollectionUtils.emptyIfNull(logShipperContext.getEnabledSensitiveStorageLogs()).contains(SensitiveLoggingComponent.SALT)) {
                builder.withIncludeSaltLogsInCloudStorageLogs();
            }
            if (logShipperContext.isPreferMinifiLogging()) {
                builder.preferMinifiLogging();
            }
        }
        return builder
                .withRegion(logShipperContext.getCloudRegion())
                .withClusterDetails(context.getClusterDetails())
                .withEnvironmentRegion(getEnvironmentRegion(context.getClusterDetails()))
                .build();
    }

    @Override
    public boolean isEnabled(TelemetryContext context) {
        return context != null && isLoggingOrMeteringEnabled(context);
    }

    @Override
    public String saltStateName() {
        return SALT_STATE;
    }

    private void setupCloudStorageLogging(FluentConfigView.Builder builder, Logging logging) {
        if (logging.getS3() != null) {
            fillS3Configs(builder, logging.getStorageLocation());
            builder.withCloudStorageLoggingEnabled(true);
            LOGGER.debug("Fluent will be configured to use S3 output.");
        } else if (logging.getAdlsGen2() != null) {
            fillAdlsGen2Configs(builder, logging.getStorageLocation(), logging.getAdlsGen2());
            builder.withCloudStorageLoggingEnabled(true);
            LOGGER.debug("Fluent will be configured to use ADLS Gen2 output.");
        } else if (logging.getGcs() != null) {
            fillGcsConfigs(builder, logging.getStorageLocation(), logging.getGcs());
            builder.withCloudStorageLoggingEnabled(true);
            LOGGER.debug("Fluent will be configured to use GCS output");
        }
    }

    private boolean isLoggingOrMeteringEnabled(TelemetryContext context) {
        return  isLoggingEnabled(context.getLogShipperContext());
    }

    private boolean isLoggingEnabled(LogShipperContext logShipperContext) {
        return logShipperContext != null && logShipperContext.isEnabled();
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

    private void fillGcsConfigs(FluentConfigView.Builder builder, String storageLocation,
            GcsCloudStorageV1Parameters parameters) {
        GcsConfig gcsConfig = gcsConfigGenerator.generateStorageConfig(storageLocation, parameters.getServiceAccountEmail());
        builder.withProviderPrefix(GCS_PROVIDER_PREFIX)
                .withGcsBucket(gcsConfig.getBucket())
                .withGcsProjectId(gcsConfig.getProjectId())
                .withLogFolderName(gcsConfig.getFolderPrefix());
    }

    private String getEnvironmentRegion(TelemetryClusterDetails clusterDetails) {
        String environmentRegion = null;
        if (clusterDetails != null && StringUtils.isNotBlank(clusterDetails.getCrn())) {
            Crn crn = Crn.fromString(clusterDetails.getCrn());
            if (crn != null && crn.getRegion() != null) {
                environmentRegion = crn.getRegion().getName();
                LOGGER.debug("Found environment region for telemetry: {}", environmentRegion);
            } else {
                LOGGER.debug("CRN is not filled correctly for telemetry cluster details");
            }
        }
        return environmentRegion;
    }
}
