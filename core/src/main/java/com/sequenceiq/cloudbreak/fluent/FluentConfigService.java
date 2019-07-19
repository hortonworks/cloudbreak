package com.sequenceiq.cloudbreak.fluent;

import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.fluent.cloud.S3Config;
import com.sequenceiq.cloudbreak.fluent.cloud.S3ConfigGenerator;
import com.sequenceiq.cloudbreak.fluent.cloud.WasbConfig;
import com.sequenceiq.cloudbreak.fluent.cloud.WasbConfigGenerator;
import com.sequenceiq.common.api.cloudstorage.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class FluentConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FluentConfigService.class);

    private static final String CLUSTER_TYPE_DISTROX = "datahub";

    private static final String CLUSTER_TYPE_SDX = "datalake";

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

    public FluentConfigView createFluentConfigs(Stack stack, Telemetry telemetry) {
        final FluentConfigView.Builder builder = new FluentConfigView.Builder();
        boolean fluentEnabled = false;
        if (telemetry != null && telemetry.getLogging() != null) {
            Logging logging = telemetry.getLogging();
            String clusterName = stack.getCluster().getName();
            String platform = stack.getCloudPlatform();
            String storageLocation = logging.getStorageLocation();

            String clusterType = StackType.DATALAKE.equals(stack.getType()) ? CLUSTER_TYPE_SDX : CLUSTER_TYPE_DISTROX;
            String logFolderName = Paths.get(CLUSTER_LOG_PREFIX, clusterType, clusterName).toString();

            builder.withPlatform(platform)
                    .withOverrideAttributes(
                            logging.getAttributes() != null ? new HashMap<>(logging.getAttributes()) : new HashMap<>()
                    )
                    .withProviderPrefix(DEFAULT_PROVIDER_PREFIX);

            if (logging.getS3() != null) {
                fillS3Configs(builder, storageLocation, clusterType, clusterName, logFolderName);
                LOGGER.debug("Fluent will be configured to use S3 output.");
                fluentEnabled = true;
            } else if (logging.getWasb() != null) {
                fillWasbConfigs(builder, storageLocation, logging.getWasb(), clusterType, clusterName, logFolderName);
                LOGGER.debug("Fluent will be configured to use WASB output.");
                fluentEnabled = true;
            }
        }
        if (!fluentEnabled) {
            LOGGER.debug("Fluent usage is disabled");
        }

        return builder
                .withEnabled(fluentEnabled)
                .build();
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
